package dk.digitalidentity.sofd.dao.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.envers.Audited;

import dk.digitalidentity.sofd.dao.model.enums.AccessEntity;
import dk.digitalidentity.sofd.dao.model.enums.AccessEntityField;
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
