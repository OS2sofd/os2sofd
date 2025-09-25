package dk.digitalidentity.sofd.dao.model.mapping;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

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
