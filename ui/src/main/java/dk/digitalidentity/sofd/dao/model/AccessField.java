package dk.digitalidentity.sofd.dao.model;

import org.hibernate.envers.Audited;

import dk.digitalidentity.sofd.dao.model.enums.AccessEntity;
import dk.digitalidentity.sofd.dao.model.enums.AccessEntityField;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Audited
@Entity
@EqualsAndHashCode(of = { "accessEntityField", "entity" })
public class AccessField {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="client_id")
	private Client client;

	@Column
	@Enumerated(EnumType.STRING)
	private AccessEntity entity;

	@Column(name="field")
	@Enumerated(EnumType.STRING)
	private AccessEntityField accessEntityField;

}
