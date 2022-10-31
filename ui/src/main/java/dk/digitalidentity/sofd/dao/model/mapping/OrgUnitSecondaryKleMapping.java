package dk.digitalidentity.sofd.dao.model.mapping;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;

import org.hibernate.envers.Audited;

import dk.digitalidentity.sofd.dao.model.OrgUnit;
import lombok.Getter;
import lombok.Setter;

@Audited
@Entity(name = "orgunits_kle_secondary")
@Getter
@Setter
public class OrgUnitSecondaryKleMapping {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "orgunit_uuid")
	@NotNull
	private OrgUnit orgUnit;

	@Column
	private String kleValue;
}
