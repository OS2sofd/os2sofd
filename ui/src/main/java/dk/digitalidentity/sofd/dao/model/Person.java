package dk.digitalidentity.sofd.dao.model;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.checkerframework.checker.units.qual.C;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import dk.digitalidentity.sofd.dao.model.enums.EntityType;
import dk.digitalidentity.sofd.dao.model.mapping.PersonAuthorizationCodeMapping;
import dk.digitalidentity.sofd.dao.model.mapping.PersonPhoneMapping;
import dk.digitalidentity.sofd.dao.model.mapping.PersonUserMapping;
import dk.digitalidentity.sofd.log.Loggable;
import dk.digitalidentity.sofd.serializer.LocalExtensionsDeserializer;
import dk.digitalidentity.sofd.serializer.LocalExtensionsSerializer;
import dk.digitalidentity.sofd.service.PersonService;
import dk.digitalidentity.sofd.service.SupportedUserTypeService;
import lombok.Getter;
import lombok.Setter;

@Entity(name = "persons")
@Getter
@Setter
@Audited
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" }) // need this because we sometimes detach the object from Hibernate
public class Person implements Loggable {

	@Id
	private String uuid;
	
	@Column
	@NotNull
	private String master;

	@CreationTimestamp
	@Temporal(TemporalType.TIMESTAMP)
	@Column(updatable = false)
	private Date created;

	// not audited because we have revinfo table for this information
	@NotAudited
	@Temporal(TemporalType.TIMESTAMP)
	@Column
	private Date lastChanged;

	@Column
	private boolean deleted;

	@Column
	@Size(min = 10, max = 10)
	@NotNull
	private String cpr;

	@Column
	@NotNull
	@Size(max = 255)
	private String firstname;

	@Column
	@NotNull
	@Size(max = 255)
	private String surname;

	@Column
	@Size(max = 255)
	private String chosenName;

	@NotAudited
	@Column
	private String keyWords;

	@NotAudited
	@Column
	private String notes;

	@Temporal(TemporalType.DATE)
	@Column
	private Date firstEmploymentDate;

	@Temporal(TemporalType.DATE)
	@Column
	private Date anniversaryDate;

	@NotAudited
	@Column
	private String stopReason;

	@Column
	private boolean taxedPhone;
	
	@Column
	private boolean forceStop;

	@Column
	private boolean disableAccountOrdersCreate;

	@Column
	private boolean disableAccountOrdersDisable;

	@Column
	private boolean disableAccountOrdersDelete;

	@Column
	private boolean dead;
	
	@Column
	private boolean disenfranchised;

	@Column
	private boolean hasUpdatedAuthorizationCode;

	@Column
	private boolean updatedFromCpr = false;

	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	@JoinColumn(name = "leave_id")
	@Valid
	private PersonLeave leave;

	@BatchSize(size = 100)
	@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@JoinColumn(name = "registered_post_address_id")
	@Valid
	private Post registeredPostAddress;
	
	@BatchSize(size = 100)
	@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@JoinColumn(name = "residence_post_address_id")
	@Valid
	private Post residencePostAddress;
	
	@BatchSize(size = 100)
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "person")
	@Valid
	private List<PersonPhoneMapping> phones;

	@BatchSize(size = 100)
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "person")
	@Valid
	private List<PersonUserMapping> users;

	@BatchSize(size = 100)
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "person")
	@Valid
	private List<Affiliation> affiliations;

	@BatchSize(size = 100)
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "person")
	@Valid
	private List<SubstituteAssignment> substitutes;

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "parent", orphanRemoval = true)
	@Valid
	private List<Child> children;

	@BatchSize(size = 100)
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "person")
	@Valid
	private List<PersonAuthorizationCodeMapping> authorizationCodes;

	@Column
	@JsonSerialize(using = LocalExtensionsSerializer.class)
	@JsonDeserialize(using = LocalExtensionsDeserializer.class)
	private String localExtensions;

	@JsonIgnore
	public boolean isOnActiveLeave() {
		if (this.leave == null) {
			return false;
		}
		
		if (this.leave.getStartDate() != null) {
			Date now = new Date();
			
			if (!now.after(this.leave.getStartDate())) {
				return false;
			}
		}
		
		return true;
	}

	@JsonIgnore
	public List<User> onlyActiveUsers() {
		return PersonService.getUsers(this).stream().filter(u -> u.isDisabled() == false).collect(Collectors.toList());
	}

	@JsonIgnore
	public String getPrimeADAccount() {
		Optional<User> primeAD = PersonService.getUsers(this).stream().filter(u -> u.isPrime() && SupportedUserTypeService.isActiveDirectory(u.getUserType())).findFirst();

		if (primeAD.isPresent()) {
			return primeAD.get().getUserId();
		}
		
		return null;
	}

	@JsonIgnore
	public String getActiveADAccounts() {
		var adAccounts = PersonService.getUsers(this).stream().filter(u -> SupportedUserTypeService.isActiveDirectory(u.getUserType()) && !u.isDisabled()).map(User::getUserId).collect(Collectors.joining(", "));
		return adAccounts;
	}

	@JsonIgnore
	public String getPrimeUserByUserType(String userType) {
		Optional<User> primeUser = PersonService.getUsers(this).stream().filter(u -> u.isPrime() && Objects.equals(u.getUserType(),userType)).findFirst();

		if (primeUser.isPresent()) {
			return primeUser.get().getUserId();
		}

		return null;
	}

	@JsonIgnore
	public String getPrimeOPUSAccount() {
		Optional<User> primeOPUS = PersonService.getUsers(this).stream().filter(u -> u.isPrime() && SupportedUserTypeService.isOpus(u.getUserType())).findFirst();

		if (primeOPUS.isPresent()) {
			return primeOPUS.get().getUserId();
		}
		
		return null;
	}

	@JsonIgnore
	public Affiliation getPrimeAffiliation() {
		if (this.affiliations != null) {
			for (Affiliation affiliation : affiliations) {
				if (affiliation.isPrime()) {
					return affiliation;
				}
			}
		}
		
		return null;
	}

	@JsonIgnore
	@Override
	public String getEntityId() {
		return uuid;
	}
	
	@JsonIgnore
	@Override
	public EntityType getEntityType() {
		return EntityType.PERSON;
	}
		
    @Override
    public boolean equals(Object o) {
        if (o == null) {
        	return false;
        }

        if (this == o) {
        	return true;
        }

        if (!Objects.equals(getClass(), o.getClass())) {
            return false;
        }

        Person that = (Person) o;

        return this.uuid != null && Objects.equals(this.uuid, that.uuid);
    }

    @Override
    public int hashCode() {

    	// this is always non-null
    	if (this.cpr != null) {
    		return cpr.hashCode();
    	}
    	
    	// but safety first
        return getClass().hashCode();
    }

	@Override
	public String getEntityName() {
		return PersonService.getName(this);
	}

	@JsonIgnore
	public boolean hasFictionalCpr() {
		return PersonService.isFictionalCpr(this.cpr);
	}

	@JsonIgnore
	public boolean hasName() {
		return this.firstname != null && !this.firstname.equalsIgnoreCase("Ukendt");
	}
}
