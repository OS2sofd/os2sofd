package dk.digitalidentity.sofd.controller.api.v2.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonFormat;

import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.Phone;
import dk.digitalidentity.sofd.dao.model.User;
import dk.digitalidentity.sofd.dao.model.mapping.PersonAuthorizationCodeMapping;
import dk.digitalidentity.sofd.dao.model.mapping.PersonPhoneMapping;
import dk.digitalidentity.sofd.dao.model.mapping.PersonUserMapping;
import dk.digitalidentity.sofd.service.PersonService;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(exclude = { "cpr", "localExtensions" })
@NoArgsConstructor
public class PersonApiRecord extends BaseRecord {

	// primary key
	@Pattern(regexp = "([0-9]{10})", message = "Invalid cpr")
	private String cpr;

	// read/write fields

	@NotNull
	private String master;

	@NotNull
	private String firstname;
	
	@NotNull
	private String surname;

	@Valid
	private PostApiRecord registeredPostAddress;
	
	@Valid
	private PostApiRecord residencePostAddress;
	
	@Valid
	private Set<PhoneApiRecord> phones;
	
	@Valid
	private Set<UserApiRecord> users;
	
	@Valid
	private Set<AffiliationApiRecord> affiliations;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private LocalDate firstEmploymentDate;
	
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private LocalDate anniversaryDate;

	private String chosenName;
	private Map<String, Object> localExtensions;
	
	// readonly

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime created;
	
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime lastChanged;

	private Set<AuthorizationCodeApiRecord> authorizationCodes;
	private boolean deleted;
	private String uuid;
	private String authorizationCode;
	private String keyWords;
	private LeaveApiRecord leave;
	private boolean disableAccountOrdersCreate;
	private boolean disableAccountOrdersDisable;
	private boolean disableAccountOrdersDelete;

	public PersonApiRecord(Person person) {
		this.cpr = person.getCpr();
		this.uuid = person.getUuid();
		this.master = person.getMaster();
		this.created = (person.getCreated() != null) ? toLocalDateTime(person.getCreated()) : null;
		this.lastChanged = (person.getLastChanged() != null) ? toLocalDateTime(person.getLastChanged()) : null;
		this.deleted = person.isDeleted();
		this.firstname = person.getFirstname();
		this.surname = person.getSurname();
		this.chosenName = person.getChosenName();
		this.firstEmploymentDate = (person.getFirstEmploymentDate() != null) ? toLocalDate(person.getFirstEmploymentDate()) : null;
		this.anniversaryDate = (person.getAnniversaryDate() != null) ? toLocalDate(person.getAnniversaryDate()) : null;
		this.registeredPostAddress = (person.getRegisteredPostAddress() != null) ? new PostApiRecord(person.getRegisteredPostAddress()) : null;
		this.residencePostAddress = (person.getResidencePostAddress() != null) ? new PostApiRecord(person.getResidencePostAddress()) : null;
		this.localExtensions = stringToMap(person.getLocalExtensions());
		this.keyWords = person.getKeyWords();

		if (person.getPhones() != null) {
			this.phones = new HashSet<PhoneApiRecord>();

			for (Phone phone : PersonService.getPhones(person)) {
				this.phones.add(new PhoneApiRecord(phone));
			}
		}
		
		if (person.getUsers() != null) {
			this.users = new HashSet<UserApiRecord>();

			for (User user : PersonService.getUsers(person)) {
				this.users.add(new UserApiRecord(user));
			}
		}
		
		if (person.getAffiliations() != null) {
			this.affiliations = new HashSet<AffiliationApiRecord>();

			for (Affiliation affiliation : person.getAffiliations()) {
				this.affiliations.add(new AffiliationApiRecord(affiliation));
			}
		}

		if (person.getAuthorizationCodes() != null) {
			this.authorizationCode = person.getAuthorizationCodes().stream().filter(a -> a.getAuthorizationCode().isPrime()).map(a -> a.getAuthorizationCode().getCode()).findAny().orElse(null);
			this.authorizationCodes = new HashSet<AuthorizationCodeApiRecord>();

			for (PersonAuthorizationCodeMapping authorizationCodeMapping : person.getAuthorizationCodes()) {
				this.authorizationCodes.add(new AuthorizationCodeApiRecord(authorizationCodeMapping.getAuthorizationCode()));
			}
		}

		if(person.getLeave()!= null) {
			this.leave = new LeaveApiRecord(person.getLeave());
		}

		this.disableAccountOrdersCreate = person.isDisableAccountOrdersCreate();
		this.disableAccountOrdersDisable = person.isDisableAccountOrdersDisable();
		this.disableAccountOrdersDelete = person.isDisableAccountOrdersDelete();

	}

	public Person toPerson(Person actualPerson, String seedPrefix) {
		Person person = new Person();
		
		if (actualPerson == null) {
			actualPerson = person;
		}

		person.setUuid(uuid);
		person.setAnniversaryDate(toDate(anniversaryDate));
		person.setChosenName(chosenName);
		person.setCpr(cpr);
		person.setFirstEmploymentDate(toDate(firstEmploymentDate));
		person.setFirstname(firstname);
		person.setLocalExtensions(mapToString(localExtensions));
		person.setMaster(master);
		person.setRegisteredPostAddress((registeredPostAddress != null) ? registeredPostAddress.toPost() : null);
		person.setResidencePostAddress((residencePostAddress != null) ? residencePostAddress.toPost() : null);
		person.setSurname(surname);

		if (affiliations != null) {
			person.setAffiliations(new ArrayList<>());

			for (AffiliationApiRecord affiliationRecord : affiliations) {
				person.getAffiliations().add(affiliationRecord.toAffiliation(actualPerson));
			}
		}
		
		if (phones != null) {
			person.setPhones(new ArrayList<>());

			for (PhoneApiRecord phoneRecord : phones) {
				PersonPhoneMapping mapping = new PersonPhoneMapping();
				mapping.setPerson(actualPerson);
				mapping.setPhone(phoneRecord.toPhone());
				
				person.getPhones().add(mapping);
			}
		}

		if (users != null) {
			person.setUsers(new ArrayList<>());

			for (UserApiRecord userRecord : users) {
				PersonUserMapping mapping = new PersonUserMapping();
				mapping.setPerson(actualPerson);
				mapping.setUser(userRecord.toUser(seedPrefix));
				
				person.getUsers().add(mapping);
			}
		}

		if (authorizationCodes != null) {
			person.setAuthorizationCodes(new ArrayList<>());

			for (AuthorizationCodeApiRecord authCodeRecord : authorizationCodes) {
				PersonAuthorizationCodeMapping mapping = new PersonAuthorizationCodeMapping();
				mapping.setPerson(actualPerson);
				mapping.setAuthorizationCode(authCodeRecord.toAuthorizationCode());

				person.getAuthorizationCodes().add(mapping);
			}
		}

		return person;
	}
}
