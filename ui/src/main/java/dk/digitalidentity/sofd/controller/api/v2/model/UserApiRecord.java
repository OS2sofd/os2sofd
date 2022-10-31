package dk.digitalidentity.sofd.controller.api.v2.model;

import java.util.Map;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import dk.digitalidentity.sofd.dao.model.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(exclude = { "localExtensions" })
@NoArgsConstructor
public class UserApiRecord extends BaseRecord {
	
	// primary key
	
	@NotNull
	private String master;
	
	@NotNull
	private String masterId;

	// read/write fields

	@Pattern(regexp = "([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})", message = "Invalid uuid")
	private String uuid;
	
	@NotNull
	private String userId;
	
	@NotNull
	private String userType;

	private String employeeId;
	
	private Map<String, Object> localExtensions;

	private Boolean disabled;
	
	private Boolean substituteAccount;
	
	// these fields are only relevant for AD accounts, they are NULL for all others
	// note that neither the fancy converter-constructor, nor the "toUser" does with this
	// in any meaningful way, as they are used 100% outside the scope of the person/user construct

	private Boolean passwordLocked;
	private String accountExpireDate;
	private String passwordExpireDate;
	private String upn;

	// read-only fields

	private boolean prime;

	public UserApiRecord(User user) {
		this.master = user.getMaster();
		this.masterId = user.getMasterId();
		this.uuid = user.getUuid();
		this.userId = user.getUserId();
		this.employeeId = user.getEmployeeId();
		this.userType = user.getUserType();
		this.localExtensions = stringToMap(user.getLocalExtensions());
		this.prime = user.isPrime();
		this.disabled = user.isDisabled();
		this.substituteAccount = user.isSubstituteAccount();
				
		// readonly fields mapping for AD accounts
		if (user.getActiveDirectoryDetails() != null) {
			this.passwordLocked = user.getActiveDirectoryDetails().isPasswordLocked();
			this.accountExpireDate = (user.getActiveDirectoryDetails().getAccountExpireDate() != null) ? user.getActiveDirectoryDetails().getAccountExpireDate().toString() : null;
			this.passwordExpireDate = (user.getActiveDirectoryDetails().getPasswordExpireDate() != null) ? user.getActiveDirectoryDetails().getPasswordExpireDate().toString() : null;
			this.upn = user.getActiveDirectoryDetails().getUpn();
		}
	}

	public User toUser() {
		User user = new User();
		user.setEmployeeId(("".equals(employeeId)) ? null : employeeId); // translate "" to null on incoming data for employeeId
		user.setLocalExtensions(mapToString(localExtensions));
		user.setMaster(master);
		user.setMasterId(masterId);
		user.setUserId(userId);
		user.setUserType(userType);
		user.setUuid(uuid);
		
		// this is not actually true (might be different in the DB on a patch call) - but these fields are never accessed in the comparison,
		// and only set when creating, so the default values of FALSE makes sense if not supplied by the caller
		user.setDisabled((disabled != null) ? disabled : false);
		user.setSubstituteAccount((substituteAccount != null) ? substituteAccount : false);

		// transient fields, to ensure correct validation in BetterAPI (could be removed if we find a better way for BetterAPI to deal with patching booleans)
		user.setTDisabled(disabled);
		user.setTSubstituteAccount(substituteAccount);

		return user;
	}
}
