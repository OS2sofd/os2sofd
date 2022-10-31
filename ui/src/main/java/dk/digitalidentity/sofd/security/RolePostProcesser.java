package dk.digitalidentity.sofd.security;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import dk.digitalidentity.samlmodule.model.SamlGrantedAuthority;
import dk.digitalidentity.samlmodule.model.SamlLoginPostProcessor;
import dk.digitalidentity.samlmodule.model.TokenUser;
import dk.digitalidentity.sofd.config.RoleConstants;
import dk.digitalidentity.sofd.config.SessionConstants;
import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.service.NotificationService;

@Component
public class RolePostProcesser implements SamlLoginPostProcessor {

	@Autowired
	private SofdConfiguration configuration;
	
	@Autowired
	private NotificationService adminTaskService;
	
	@Override
	public void process(TokenUser tokenUser) {
		Set<SamlGrantedAuthority> newAuthorities = new HashSet<>();

		if (configuration.getModules().getAccountCreation().isEnabled()) {
			newAuthorities.add(new SamlGrantedAuthority(RoleConstants.MODULE_ROLE_ACCOUNT_CREATION, null, null));
		}
		
		if (configuration.getModules().getProfile().isEnabled()) {
			newAuthorities.add(new SamlGrantedAuthority(RoleConstants.MODULE_ROLE_PROFILE, null, null));
		}
		
		if (configuration.getModules().getSmsGateway().isEnabled()) {
			newAuthorities.add(new SamlGrantedAuthority(RoleConstants.MODULE_ROLE_SMS_GATEWAY, null, null));
		}
		
		if (configuration.getModules().getPersonComments().isEnabled()) {
			newAuthorities.add(new SamlGrantedAuthority(RoleConstants.MODULE_ROLE_PERSON_COMMENT, null, null));
		}
		
		if (configuration.getModules().getLos().isEnabled()) {
			newAuthorities.add(new SamlGrantedAuthority(RoleConstants.MODULE_ROLE_LOS, null, null));
		}
		
		if (configuration.getModules().getTelephony().isEnabled()) {
			newAuthorities.add(new SamlGrantedAuthority(RoleConstants.MODULE_ROLE_TELEPHONY, null, null));
		}

		boolean canSeeNotifications = false;
		for (Iterator<SamlGrantedAuthority> iterator = tokenUser.getAuthorities().iterator(); iterator.hasNext();) {
			SamlGrantedAuthority grantedAuthority = iterator.next();
			
			if ("ROLE_admin".equals(grantedAuthority.getAuthority())) {
				newAuthorities.add(new SamlGrantedAuthority(RoleConstants.USER_ROLE_ADMIN, null, null));
				newAuthorities.add(new SamlGrantedAuthority(RoleConstants.USER_ROLE_SMS, null, null));
				newAuthorities.add(new SamlGrantedAuthority(RoleConstants.USER_ROLE_EDIT, null, null));
				newAuthorities.add(new SamlGrantedAuthority(RoleConstants.USER_ROLE_READ, null, null));
				newAuthorities.add(new SamlGrantedAuthority(RoleConstants.USER_ROLE_EDIT_CONTACT_INFO, null, null));
				
				if (configuration.getModules().getLos().isEnabled()) {
					newAuthorities.add(new SamlGrantedAuthority(RoleConstants.USER_ROLE_LOS_ADMIN, null, null));
				}

				if (configuration.getModules().getTelephony().isEnabled()) {
					newAuthorities.add(new SamlGrantedAuthority(RoleConstants.USER_ROLE_TELEPHONY, null, null));
				}

				canSeeNotifications = true;
			}
			else if ("ROLE_losadmin".equals(grantedAuthority.getAuthority())) {
				newAuthorities.add(new SamlGrantedAuthority(RoleConstants.USER_ROLE_EDIT, null, null));
				newAuthorities.add(new SamlGrantedAuthority(RoleConstants.USER_ROLE_READ, null, null));
				newAuthorities.add(new SamlGrantedAuthority(RoleConstants.USER_ROLE_EDIT_CONTACT_INFO, null, null));

				if (configuration.getModules().getLos().isEnabled()) {
					newAuthorities.add(new SamlGrantedAuthority(RoleConstants.USER_ROLE_LOS_ADMIN, null, null));
				}

				canSeeNotifications = true;
			}
			else if ("ROLE_edit".equals(grantedAuthority.getAuthority())) {
				newAuthorities.add(new SamlGrantedAuthority(RoleConstants.USER_ROLE_EDIT, null, null));
				newAuthorities.add(new SamlGrantedAuthority(RoleConstants.USER_ROLE_READ, null, null));
				newAuthorities.add(new SamlGrantedAuthority(RoleConstants.USER_ROLE_EDIT_CONTACT_INFO, null, null));
				
				canSeeNotifications = true;
			}
			else if ("ROLE_edit_contact_info".equals(grantedAuthority.getAuthority())) {
				newAuthorities.add(new SamlGrantedAuthority(RoleConstants.USER_ROLE_READ, null, null));
				newAuthorities.add(new SamlGrantedAuthority(RoleConstants.USER_ROLE_EDIT_CONTACT_INFO, null, null));
			}
			else if ("ROLE_telephonyadmin".equals(grantedAuthority.getAuthority())) {
				newAuthorities.add(new SamlGrantedAuthority(RoleConstants.USER_ROLE_READ, null, null));

				if (configuration.getModules().getTelephony().isEnabled()) {
					newAuthorities.add(new SamlGrantedAuthority(RoleConstants.USER_ROLE_TELEPHONY, null, null));
				}
			}
			else if ("ROLE_sms".equals(grantedAuthority.getAuthority())) {
				newAuthorities.add(new SamlGrantedAuthority(RoleConstants.USER_ROLE_READ, null, null));

				if (configuration.getModules().getSmsGateway().isEnabled()) {
					newAuthorities.add(new SamlGrantedAuthority(RoleConstants.USER_ROLE_SMS, null, null));
				}
			}
			else if ("ROLE_read".equals(grantedAuthority.getAuthority())) {
				newAuthorities.add(new SamlGrantedAuthority(RoleConstants.USER_ROLE_READ, null, null));
			}
			else if ("ROLE_cpr_access".equals(grantedAuthority.getAuthority())) {
				newAuthorities.add(new SamlGrantedAuthority(RoleConstants.USER_ROLE_CPR_ACCESS, null, null));
			}
			else if ("ROLE_person_creater".equals(grantedAuthority.getAuthority())) {
				newAuthorities.add(new SamlGrantedAuthority(RoleConstants.USER_ROLE_PERSON_CREATER, grantedAuthority.getConstraints(), grantedAuthority.getScope()));
				newAuthorities.add(new SamlGrantedAuthority(RoleConstants.USER_ROLE_READ, null, null));
			}
		}

		tokenUser.setAuthorities(newAuthorities);
		
		if (canSeeNotifications) {
			setNotifications();
		}
	}

	private void setNotifications() {
		HttpServletRequest request = getRequest();
		
		if (request != null) {
			long count = adminTaskService.countActive();
			request.getSession().setAttribute(SessionConstants.SESSION_NOTIFICATION_COUNT, count);
		}
	}
	
	private static HttpServletRequest getRequest() {
		try {
			return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
		}
		catch (IllegalStateException ex) {
			return null;
		}
	}
}
