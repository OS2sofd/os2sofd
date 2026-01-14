package dk.digitalidentity.sofd.dao.model.mapping;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import dk.digitalidentity.sofd.dao.model.ContactPlace;
import dk.digitalidentity.sofd.dao.model.OrgUnit;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "contactplaces_orgunits")
@Getter
@Setter
public class ContactPlaceOrgUnitMapping {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "orgunit_uuid")
	@NotNull
	private OrgUnit orgUnit;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "contactplace_id")
	@NotNull
	private ContactPlace contactPlace;
	
	@Column
	private boolean deleted;
}
