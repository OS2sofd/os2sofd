package dk.digitalidentity.sofd.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import dk.digitalidentity.sofd.service.IpWhitelistService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;

import dk.digitalidentity.samlmodule.model.SamlGrantedAuthority;
import dk.digitalidentity.sofd.dao.model.Client;
import dk.digitalidentity.sofd.dao.model.enums.AccessRole;
import dk.digitalidentity.sofd.service.ClientService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ApiSecurityFilter implements Filter {
	private final ClientService clientService;
	private final IpWhitelistService ipWhitelistService;

	public ApiSecurityFilter(ClientService clientService, IpWhitelistService ipWhitelistService) {
		this.clientService = clientService;
		this.ipWhitelistService = ipWhitelistService;
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) servletRequest;
		HttpServletResponse response = (HttpServletResponse) servletResponse;

		// we are using a custom header instead of Authorization because the Authorization header plays very badly with the SAML filter
		String authHeader = request.getHeader("ApiKey");
		String versionHeader = request.getHeader("ClientVersion");
		
		// accept our own custom TlsVersion header first (from middleware that forwards information from local agent),
		// and then fallback to load balancer header
		String tlsVersion = request.getHeader("TlsVersion");
		if (!StringUtils.hasLength(tlsVersion)) {
			tlsVersion = request.getHeader("x-amzn-tls-version");
		}
		
		if (StringUtils.hasLength(tlsVersion)) {
			tlsVersion = (tlsVersion.length() > 64) ? (tlsVersion.substring(0, 60) + "...") : tlsVersion;
		}
		else {
			tlsVersion = null;
		}

		if (authHeader != null) {
			Client client = clientService.getClientByApiKey(authHeader);
			if (client == null) {
				unauthorized(response, "Invalid ApiKey header", authHeader);
				return;
			}

			if (!ipWhitelistService.isWhitelisted(client)) {
				unauthorized(response, "IP not whitelisted", authHeader);
				return;
			}

			ArrayList<GrantedAuthority> authorities = new ArrayList<>();
			switch (client.getAccessRole()) {
				case WRITE_ACCESS:
					authorities.add(new SamlGrantedAuthority("ROLE_" + AccessRole.WRITE_ACCESS.toString(), null, null));
					authorities.add(new SamlGrantedAuthority("ROLE_" + AccessRole.READ_ACCESS.toString(), null, null));
					break;
				case READ_ACCESS:
					authorities.add(new SamlGrantedAuthority("ROLE_" + AccessRole.READ_ACCESS.toString(), null, null));
					break;
				case LIMITED_READ_ACCESS:
					authorities.add(new SamlGrantedAuthority("ROLE_" + AccessRole.LIMITED_READ_ACCESS.toString(), null, null));
					break;
			}

			boolean clientChanged = false;
			
			if (versionHeader != null && !Objects.equals(client.getVersion(), versionHeader)) {
				client.setVersion(versionHeader);

				clientChanged = true;
			}
			
			if (tlsVersion != null && !Objects.equals(client.getTlsVersion(), tlsVersion)) {
				client.setTlsVersion(tlsVersion);
				clientChanged = true;
			}
			
			if (clientChanged) {
				Client clientFromDb = clientService.getClientById(client.getId());
				clientFromDb.setVersion(client.getVersion());
				clientFromDb.setTlsVersion(client.getTlsVersion());
				clientService.save(clientFromDb);
			}
			
			ClientToken token = new ClientToken(client.getName(), client.getApiKey(), authorities);
			token.setClient(client);

			SecurityContextHolder.getContext().setAuthentication(token);
			filterChain.doFilter(servletRequest, servletResponse);
		} else {
			unauthorized(response, "Missing ApiKey header", authHeader);
		}
	}

	private static void unauthorized(HttpServletResponse response, String message, String authHeader) throws IOException {
		log.warn(message + " (authHeader = " + authHeader + ")");
		response.sendError(401, message);
	}

	@Override
	public void destroy() {
		;
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		;
	}
}