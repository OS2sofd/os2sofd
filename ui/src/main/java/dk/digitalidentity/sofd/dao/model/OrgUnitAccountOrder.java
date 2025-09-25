package dk.digitalidentity.sofd.dao.model;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

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
