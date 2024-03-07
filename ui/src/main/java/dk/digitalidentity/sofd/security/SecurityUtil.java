package dk.digitalidentity.sofd.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import dk.digitalidentity.samlmodule.model.SamlGrantedAuthority;
import dk.digitalidentity.samlmodule.model.SamlGrantedAuthority.Constraint;
import dk.digitalidentity.samlmodule.model.TokenUser;
import dk.digitalidentity.sofd.config.RoleConstants;
import dk.digitalidentity.sofd.dao.model.Client;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.enums.AccessRole;
import dk.digitalidentity.sofd.service.PersonService;

public class SecurityUtil {

	// certain scheduled tasks needs to update data that requires WRITE_ACCESS,
	// so a fakeLoginSession can be created for this purpose, and put onto the
	// running thread
	public static void fakeLoginSession() {
		ArrayList<GrantedAuthority> authorities = new ArrayList<>();
		authorities.add(new SamlGrantedAuthority("ROLE_" + AccessRole.WRITE_ACCESS.toString(), null, null));

		// the client is not persisted, and is only created to fulfill the expectations
		// of the rest of the code
		Client client = new Client();
		client.setName("SystemTask");
		client.setAccessRole(AccessRole.WRITE_ACCESS);
		client.setApiKey(UUID.randomUUID().toString());
		
		ClientToken token = new ClientToken(client.getName(), client.getApiKey(), authorities);
		token.setClient(client);

		SecurityContextHolder.getContext().setAuthentication(token);
	}
	
	public static Authentication getLoginSession() {
		return SecurityContextHolder.getContext().getAuthentication();
	}

	public static void setLoginSession(Authentication authentication) {
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}

	public static void loginPerson(Person person, String userId) {
		ArrayList<SamlGrantedAuthority> authorities = new ArrayList<>();
		authorities.add(new SamlGrantedAuthority(RoleConstants.USER_ROLE_SMS, null, null));
		authorities.add(new SamlGrantedAuthority(RoleConstants.USER_ROLE_READ, null, null));
		authorities.add(new SamlGrantedAuthority(RoleConstants.MODULE_ROLE_SMS_GATEWAY, null, null));

		UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(PersonService.getName(person), null, authorities);
		token.setDetails(TokenUser.builder().username(userId).attributes(new HashMap<>()).authorities(authorities).build());

		SecurityContextHolder.getContext().setAuthentication(token);
	}

	public static String getUser() {
		String name = null;

		// if a client is logged in, a user is NOT logged in
		if (getClient() != null) {
			return null;
		}

		if (isUserLoggedIn()) {
			name = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		}

		return name;
	}

	public static Client getClient() {
		Client client = null;

		if (isClientLoggedIn()) {
			client = ((ClientToken) SecurityContextHolder.getContext().getAuthentication()).getClient();
		}

		return client;
	}

	public static List<String> getUserRoles() {
		List<String> roles = new ArrayList<>();

		if (isUserLoggedIn()) {
			for (GrantedAuthority grantedAuthority : (SecurityContextHolder.getContext().getAuthentication()).getAuthorities()) {
				roles.add(grantedAuthority.getAuthority());
			}
		}

		return roles;
	}

	public static boolean isUserLoggedIn() {
		if (isLoggedIn() && SecurityContextHolder.getContext().getAuthentication() instanceof UsernamePasswordAuthenticationToken) {
			return true;
		}
		
		return false;
	}
	
	public static boolean isClientLoggedIn() {
		if (isLoggedIn() && SecurityContextHolder.getContext().getAuthentication() instanceof ClientToken) {
			return true;
		}
		
		return false;
	}
	
	@SuppressWarnings("unchecked")
	public static Set<String> getOrgUnitUuidsFromConstraint() {
		Set<String> orgUnitUuids = new HashSet<>();

		if (isUserLoggedIn()) {
			for (SamlGrantedAuthority authority : (List<SamlGrantedAuthority>) SecurityContextHolder.getContext().getAuthentication().getAuthorities()) {
				if (Objects.equals(authority.getAuthority(), RoleConstants.USER_ROLE_PERSON_CREATER) && authority.getConstraints() != null && authority.getConstraints().size() > 0) {
					for (Constraint constraint : authority.getConstraints()) {
						orgUnitUuids.addAll(Arrays.asList(constraint.getConstraintValue().split(",")));						
					}
				}
			}
		}

		return orgUnitUuids;
	}
	
	public static boolean hasRole(String role) {
		boolean hasRole = false;

		if (isUserLoggedIn()) {
			for (GrantedAuthority authority : SecurityContextHolder.getContext().getAuthentication().getAuthorities()) {
				if (authority.getAuthority().equals(role)) {
					hasRole = true;
				}
			}
		}

		return hasRole;
	}
	
	private static boolean isLoggedIn() {
		if (SecurityContextHolder.getContext().getAuthentication() != null/* && SecurityContextHolder.getContext().getAuthentication() instanceof ClientToken*/) {
			return true;
		}

		return false;
	}
}
