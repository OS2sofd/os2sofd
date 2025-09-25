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
import javax.validation.constraints.NotNull;

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
