package dk.digitalidentity.sofd.dao.model.mapping;

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
import dk.digitalidentity.sofd.dao.model.Kle;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "contactplaces_kle")
@Getter
@Setter
public class ContactPlaceKleMapping {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "kle_id")
	@NotNull
	private Kle kle;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "contactplace_id")
	@NotNull
	private ContactPlace contactPlace;
}
