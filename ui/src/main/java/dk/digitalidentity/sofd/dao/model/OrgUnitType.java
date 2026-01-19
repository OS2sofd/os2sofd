package dk.digitalidentity.sofd.dao.model;

import org.hibernate.envers.Audited;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
