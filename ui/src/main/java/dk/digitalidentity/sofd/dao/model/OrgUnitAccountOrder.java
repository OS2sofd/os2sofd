package dk.digitalidentity.sofd.dao.model;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Entity(name = "orgunit_account_order")
@Getter
@Setter
public class OrgUnitAccountOrder {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	
	@Column
	@NotNull
	private String orgunitUuid;
	
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "orgUnitAccountOrder", fetch = FetchType.EAGER)
	@Valid
	private List<OrgUnitAccountOrderType> types;
}
