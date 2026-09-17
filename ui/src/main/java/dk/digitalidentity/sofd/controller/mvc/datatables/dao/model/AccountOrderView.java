package dk.digitalidentity.sofd.controller.mvc.datatables.dao.model;

import java.util.Date;

import dk.digitalidentity.sofd.dao.model.enums.AccountOrderStatus;
import dk.digitalidentity.sofd.dao.model.enums.AccountOrderType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "view_datatables_accountorders")
public class AccountOrderView {

	@Id
	@Column
	private long id;

	// pre-formatted as yyyy-MM-dd HH:mm by the view, so sorting and searching happens in SQL
	@Column
	private String activationTimestamp;

	@Column
	private Date endDate;

	@Column
	private String personUuid;

	@Column
	private String personName;

	@Column
	private String triggerAffiliation;

	@Column
	private String userId;

	@Column
	@Enumerated(EnumType.STRING)
	private AccountOrderType orderType;

	// SupportedUserType.key
	@Column
	private String userType;

	@Column
	@Enumerated(EnumType.STRING)
	private AccountOrderStatus status;
}
