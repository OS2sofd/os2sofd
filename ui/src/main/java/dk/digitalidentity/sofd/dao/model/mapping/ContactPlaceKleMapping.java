package dk.digitalidentity.sofd.dao.model.mapping;

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
