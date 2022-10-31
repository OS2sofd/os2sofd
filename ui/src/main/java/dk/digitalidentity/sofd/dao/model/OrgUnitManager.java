package dk.digitalidentity.sofd.dao.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.envers.Audited;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Entity(name = "orgunits_manager")
@Getter
@Setter
@Audited
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class OrgUnitManager {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column
	@NotNull
	@Size(max = 255)
	private String name;

	@Valid
	@ManyToOne(fetch = FetchType.LAZY)
	private Person manager;

	@Column
	private boolean inherited;

	@OneToOne
	@JoinColumn(name = "orgunit_uuid")
	@JsonBackReference
	private OrgUnit orgUnit;

	public OrgUnitManager() {}

	public OrgUnitManager(OrgUnit orgUnit, Person person, boolean inherited) {
		this.inherited = inherited;
		this.manager = person;
		this.orgUnit = orgUnit;
		this.name = person.getChosenName() != null ? person.getChosenName() : person.getFirstname() + " " + person.getSurname();
	}

	// TODO: nope, do not use a custom equals, instead make some specific method for this,
	//       as equals is used by Hibernate
	// We need custom equals/hashCode to make sure we only match by uuid, name and inherited and not the full object
	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}

		// check for type compatibility
		if (!(other instanceof OrgUnitManager)) {
			return false;
		}

		// cast for convenience
		final OrgUnitManager orgUnitManager = (OrgUnitManager) other;

		if (orgUnitManager.getManager() == null && getManager() != null) {
			return false;
		}

		if (orgUnitManager.getManager() != null && getManager() == null) {
			return false;
		}

		if (orgUnitManager.getManager() != null && getManager() != null) {
			if (!orgUnitManager.getManager().getUuid().equals(getManager().getUuid())) {
				return false;
			}
		}

		if (orgUnitManager.isInherited() != isInherited()) {
			return false;
		}

		if (!Objects.equals(orgUnitManager.getName(),getName())) {
			return false;
		}

		// if all business fields are equal, we are equal
		return true;
	}

	// same as above - there is a right way to implement hashCode and equals for entities
	@Override
	public int hashCode() {
		int result = 0;

		// the approach seems to be to compute the hashCode on all business fields,
		// and multiply the previous result by 29 before adding the new hashCode value
		if (getManager() != null) {
			result = 29 * result + getManager().getUuid().hashCode();
		}

		if (isInherited()) {
			result = 29 * result + 1;
		}

		return result;
	}
}
