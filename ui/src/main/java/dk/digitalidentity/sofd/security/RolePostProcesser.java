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
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.User;
import dk.digitalidentity.sofd.service.NotificationService;
import dk.digitalidentity.sofd.service.OrgUnitService;
import dk.digitalidentity.sofd.service.PersonService;
import dk.digitalidentity.sofd.service.SupportedUserTypeService;
import dk.digitalidentity.sofd.service.UserService;

@Component
public class RolePostProcesser implements SamlLoginPostProcessor {

	@Autowired
	private SofdConfiguration configuration;
	
	@Autowired
	private NotificationService adminTaskService;

	@Autowired
	private PersonService personService;

	@Autowired
	private UserService userService;
	
	@Autowired
	private OrgUnitService orgUnitService;
	
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
		if (configuration.getModules().getAccountCreation().isAccountOrderApprove()) {
			User user = userService.findByUserIdAndUserType(tokenUser.getUsername(), SupportedUserTypeService.getActiveDirectoryUserType());

			if (user != null) {
				Person loggedInPerson = personService.findByUser(user);
				
				if (loggedInPerson != null) {
			        boolean isManager = (orgUnitService.getAllWhereManagerIs(loggedInPerson).size() > 0);

			        // if not a manager, perhaps they are a substitute?
			        if (!isManager) {
			        	isManager = personService.isSubstituteInSofd(loggedInPerson);
			        }
			        
			        if (isManager) {
						newAuthorities.add(new SamlGrantedAuthority(RoleConstants.DATA_ROLE_MANAGER, null, null));
						canSeeNotifications = true;
					}
				}
			}
		}

		// if a user has both constrained and non-constrained access, give non-constrained access
		boolean hasFullEditAccess = false;
		boolean hasFullLosAccess = false;
		for (Iterator<SamlGrantedAuthority> iterator = tokenUser.getAuthorities().iterator(); iterator.hasNext();) {
			SamlGrantedAuthority grantedAuthority = iterator.next();
			if ("ROLE_edit".equals(grantedAuthority.getAuthority()) && (grantedAuthority.getConstraints() == null || grantedAuthority.getConstraints().size() == 0)) {
				hasFullEditAccess = true;
			}
			else if ("ROLE_losadmin".equals(grantedAuthority.getAuthority()) && (grantedAuthority.getConstraints() == null || grantedAuthority.getConstraints().size() == 0)) {
				hasFullLosAccess = true;
			}
			else if ("ROLE_admin".equals(grantedAuthority.getAuthority())) {
				hasFullEditAccess = true;
				hasFullLosAccess = true;
			}
		}

		for (Iterator<SamlGrantedAuthority> iterator = tokenUser.getAuthorities().iterator(); iterator.hasNext();) {
			SamlGrantedAuthority grantedAuthority = iterator.next();
			
			if ("ROLE_admin".equals(grantedAuthority.getAuthority())) {
				newAuthorities.add(new SamlGrantedAuthority(RoleConstants.USER_ROLE_ADMIN, null, null));
				newAuthorities.add(new SamlGrantedAuthority(RoleConstants.USER_ROLE_SMS, null, null));
				newAuthorities.add(new SamlGrantedAuthority(RoleConstants.USER_ROLE_EDIT, null, null));
				newAuthorities.add(new SamlGrantedAuthority(RoleConstants.USER_ROLE_READ, null, null));
				newAuthorities.add(new SamlGrantedAuthority(RoleConstants.USER_ROLE_EDIT_CONTACT_INFO, null, null));
				newAuthorities.add(new SamlGrantedAuthority(RoleConstants.USER_ROLE_TELEPHONY, null, null));
				
				if (configuration.getModules().getLos().isEnabled()) {
					newAuthorities.add(new SamlGrantedAuthority(RoleConstants.USER_ROLE_LOS_ADMIN, null, null));
				}

				canSeeNotifications = true;
			}
			else if ("ROLE_losadmin".equals(grantedAuthority.getAuthority())) {
				newAuthorities.add(new SamlGrantedAuthority(RoleConstants.USER_ROLE_READ, null, null));
				newAuthorities.add(new SamlGrantedAuthority(RoleConstants.USER_ROLE_EDIT_CONTACT_INFO, null, null));

				if (hasFullEditAccess) {
					newAuthorities.add(new SamlGrantedAuthority(RoleConstants.USER_ROLE_EDIT, null, null));
				}
				else {
					newAuthorities.add(new SamlGrantedAuthority(RoleConstants.USER_ROLE_EDIT, grantedAuthority.getConstraints(), grantedAuthority.getScope()));					
				}

				if (configuration.getModules().getLos().isEnabled()) {
					if (hasFullLosAccess) {
						newAuthorities.add(new SamlGrantedAuthority(RoleConstants.USER_ROLE_LOS_ADMIN, null, null));
					}
					else {
						newAuthorities.add(new SamlGrantedAuthority(RoleConstants.USER_ROLE_LOS_ADMIN, grantedAuthority.getConstraints(), grantedAuthority.getScope()));
					}
				}

				canSeeNotifications = true;
			}
			else if ("ROLE_edit".equals(grantedAuthority.getAuthority())) {
				if (hasFullEditAccess) {
					newAuthorities.add(new SamlGrantedAuthority(RoleConstants.USER_ROLE_EDIT, null, null));
				}
				else {
					newAuthorities.add(new SamlGrantedAuthority(RoleConstants.USER_ROLE_EDIT, grantedAuthority.getConstraints(), grantedAuthority.getScope()));					
				}

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
