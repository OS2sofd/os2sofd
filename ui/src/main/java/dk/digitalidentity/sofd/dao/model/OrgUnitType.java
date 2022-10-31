package dk.digitalidentity.sofd.dao.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.envers.Audited;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

@Entity(name = "orgunit_types")
@Audited
@Getter
@Setter
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler" }) // need this because we sometimes detach the object from Hibernate
public class OrgUnitType {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column(name = "type_key")
	@NotNull
	@Size(max = 64)
	private String key;

	@Column(name = "type_value")
	@NotNull
	@Size(max = 255)
	private String value;
	
	@Column
	private boolean active;
	
	@Column
	private String extId;
}
