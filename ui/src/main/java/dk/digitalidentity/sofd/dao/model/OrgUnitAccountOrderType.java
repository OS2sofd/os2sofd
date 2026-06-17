package dk.digitalidentity.sofd.dao.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;

import dk.digitalidentity.sofd.dao.model.enums.AccountOrderDeactivateAndDeleteRule;
import dk.digitalidentity.sofd.dao.model.enums.AccountOrderRule;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
