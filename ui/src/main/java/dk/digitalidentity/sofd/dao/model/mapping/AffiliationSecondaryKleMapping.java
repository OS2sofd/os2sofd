package dk.digitalidentity.sofd.dao.model.mapping;

import org.hibernate.envers.Audited;

import dk.digitalidentity.sofd.dao.model.Affiliation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Audited
@Entity(name = "affiliations_kle_secondary")
@Getter
@Setter
public class AffiliationSecondaryKleMapping {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "affiliation_id")
	@NotNull
	private Affiliation affiliation;

	@Column
	private String kleValue;
}
