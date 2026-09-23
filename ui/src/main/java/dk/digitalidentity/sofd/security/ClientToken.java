package dk.digitalidentity.sofd.security;

import java.util.Collection;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import dk.digitalidentity.sofd.dao.model.Client;
import lombok.Getter;
import lombok.Setter;

@SuppressWarnings("serial")
public class ClientToken extends UsernamePasswordAuthenticationToken {

	@Getter
	@Setter
	private Client client;

	// optional identity of the person the calling integration acts on behalf of (from the OnBehalfOf header),
	// used for audit logging only and never for access control
	@Getter
	@Setter
	private String onBehalfOf;
	
	public ClientToken(Object principal, Object credentials, Collection<? extends GrantedAuthority> authorities) {
		super(principal, credentials, authorities);
	}
}
