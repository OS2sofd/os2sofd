package dk.digitalidentity.sofd.dao.model;

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
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonBackReference;

import dk.digitalidentity.sofd.dao.model.enums.AccountOrderRule;
import lombok.Getter;
import lombok.Setter;

@Entity(name = "orgunit_account_order_type_position")
@Getter
@Setter
public class OrgUnitAccountOrderTypePosition {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "account_order_type_id")
	@JsonBackReference
	private OrgUnitAccountOrderType orgUnitAccountOrderType;
	
	@Column
	@NotNull
	private String positionName;
	
	@Column
	@NotNull
	@Enumerated(EnumType.STRING)
	private AccountOrderRule rule;
}
