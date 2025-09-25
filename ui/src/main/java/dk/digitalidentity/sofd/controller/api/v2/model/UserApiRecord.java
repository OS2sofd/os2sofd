package dk.digitalidentity.sofd.controller.api.v2.model;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.springframework.util.StringUtils;

import dk.digitalidentity.sofd.dao.model.ActiveDirectoryDetails;
import dk.digitalidentity.sofd.dao.model.User;
import dk.digitalidentity.sofd.service.SupportedUserTypeService;
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
	private String kombitUuid;
	private String whenCreated;
	private String title;

	// read-only fields

	private boolean prime;

	public UserApiRecord(User user) {
		this.master = user.getMaster();
		this.masterId = user.getMasterId();
		this.employeeId = user.getEmployeeId();
		this.uuid = user.getUuid();
		this.userId = user.getUserId();
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
			this.whenCreated = (user.getActiveDirectoryDetails().getWhenCreated() != null) ? user.getActiveDirectoryDetails().getWhenCreated().toString() : null;
			this.upn = (StringUtils.hasLength(user.getActiveDirectoryDetails().getUpn())) ? user.getActiveDirectoryDetails().getUpn() : null;
			this.kombitUuid = user.getActiveDirectoryDetails().getKombitUuid();
			this.title = (StringUtils.hasLength(user.getActiveDirectoryDetails().getTitle())) ? user.getActiveDirectoryDetails().getTitle() : null;
		}
	}

	public User toUser(String seedPrefix) {
		User user = new User();
		user.setEmployeeId(("".equals(employeeId)) ? null : employeeId); // translate "" to null on incoming data for employeeId
		user.setLocalExtensions(mapToString(localExtensions));
		user.setMaster(master);
		user.setMasterId(masterId);
		user.setUserId(userId);
		user.setUserType(userType);
		user.setUuid(uuid);
		
		// this code is only really used during creation of a new User - the update code looks at the stored field on the userRecord
		if (SupportedUserTypeService.isActiveDirectory(userType) || SupportedUserTypeService.isActiveDirectorySchool(userType)) {
			// compute kombitUuid
			if (StringUtils.hasLength(seedPrefix)) {
				String seed = seedPrefix + user.getUserId() + user.getUserType();

				kombitUuid = UUID.nameUUIDFromBytes(seed.toLowerCase().getBytes()).toString();
			}
			else {
				kombitUuid = user.getMasterId();
			}
			
			// sanity check on dates
			if (title != null && title.length() > 100) {
				title = title.substring(0, 100);
			}
			if (!StringUtils.hasLength(whenCreated) || whenCreated.equals("9999-12-31")) {
				whenCreated = null;
			}

			// let the 9999-12-31 value pass through, we will use it as a special case when performing the update logic in PersonApi
			// so it is possible to NULL the field (patch does not currently allow null'ing)
			if (!StringUtils.hasLength(accountExpireDate)) { // || accountExpireDate.equals("9999-12-31")) {
				accountExpireDate = null;
			}
			if (!StringUtils.hasLength(passwordExpireDate)) { // || passwordExpireDate.equals("9999-12-31")) {
				passwordExpireDate = null;
			}

			ActiveDirectoryDetails details = new ActiveDirectoryDetails();
			details.setKombitUuid(kombitUuid);
			details.setPasswordLocked(passwordLocked != null ? passwordLocked : false);
			details.setPasswordLockedDate(details.isPasswordLocked() ? LocalDate.now() : null);
			details.setUpn(upn);
			details.setUser(user);
			details.setUserType(userType);
			details.setTitle(title);
			details.setPasswordExpireDate(StringUtils.hasLength(passwordExpireDate) ? LocalDate.parse(passwordExpireDate) : null);
			details.setWhenCreated(StringUtils.hasLength(whenCreated) ? LocalDate.parse(whenCreated) : null);
			details.setAccountExpireDate(StringUtils.hasLength(accountExpireDate) ? LocalDate.parse(accountExpireDate) : null);

			user.setActiveDirectoryDetails(details);
		}

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
