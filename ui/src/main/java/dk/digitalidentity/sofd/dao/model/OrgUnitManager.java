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

import org.hibernate.annotations.Formula;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

@Entity(name = "orgunits_manager")
@Getter
@Audited
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class OrgUnitManager {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	// this was initially a real database value copied from person, but is now replaced with a formula for data integrity reasons
	// the formula performs better than looking up each manager name from the referenced manager when doing full table loads.
	@Formula("(select ifnull(p.chosen_name,concat(p.firstname,' ',p.surname)) from persons p where p.uuid = manager_uuid)")
	@NotAudited
	private String name;

	// manager_uuid field exposed directly so we don't have to fetch the entire manager entity if we only need the uuid.
	@Formula("manager_uuid")
	@NotAudited
	private String managerUuid;

	@Valid
	@ManyToOne(fetch = FetchType.LAZY)
	@Setter
	private Person manager;

	@Column
	@Setter
	private boolean inherited;

	@OneToOne
	@Setter
	@JoinColumn(name = "orgunit_uuid")
	@JsonBackReference
	private OrgUnit orgUnit;

	public OrgUnitManager() {}

	public OrgUnitManager(OrgUnit orgUnit, Person person, boolean inherited) {
		this.inherited = inherited;
		this.manager = person;
		this.orgUnit = orgUnit;
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
