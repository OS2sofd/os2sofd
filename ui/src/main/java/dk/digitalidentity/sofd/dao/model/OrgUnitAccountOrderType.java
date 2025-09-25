package dk.digitalidentity.sofd.dao.model;

import java.util.List;

import javax.persistence.CascadeType;
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
import javax.persistence.OneToMany;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonBackReference;

import dk.digitalidentity.sofd.dao.model.enums.AccountOrderDeactivateAndDeleteRule;
import dk.digitalidentity.sofd.dao.model.enums.AccountOrderRule;
import lombok.Getter;
import lombok.Setter;

@Entity(name = "orgunit_account_order_type")
@Getter
@Setter
public class OrgUnitAccountOrderType {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "account_order_id")
	@JsonBackReference
	private OrgUnitAccountOrder orgUnitAccountOrder;
	
	// just a string, so no direct reference (renaming, deleting, etc will just result in filtering of this record later)
	@Column
	@NotNull
	private String userType;
	
	@Column
	@NotNull
	@Enumerated(EnumType.STRING)
	private AccountOrderRule rule;
	
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "orgUnitAccountOrderType", fetch = FetchType.EAGER)
	@Valid
	private List<OrgUnitAccountOrderTypePosition> positions;

	@Column
	private boolean requiresApproval;

	@Column
	@NotNull
	@Enumerated(EnumType.STRING)
	private AccountOrderDeactivateAndDeleteRule deactivateAndDeleteRule;
}
