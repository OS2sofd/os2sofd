package dk.digitalidentity.sofd.controller.api.dto;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import dk.digitalidentity.sofd.dao.model.AccountOrder;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.enums.AccountOrderType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AccountOrderDTO {
	private long id;

	// which person/affiliation is the account linked to

	private PersonDTO person;

	// who ordered it, if any
	private String orderedBy;

	// details on the actual order
	
	private String userType;
	private AccountOrderType orderType;
	private String userId;
	private String linkedUserId;
	private String status;

	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date activationTimestamp;

	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date endDate;
	
	private String optionalJson;

	public AccountOrderDTO(AccountOrder order, String orderedBy, Person person, boolean encodeCpr) {
		this.id = order.getId();
		this.person = new PersonDTO(person, order.getEmployeeId(), encodeCpr);
		this.userType = order.getUserType();
		this.orderedBy = orderedBy;
		this.orderType = order.getOrderType();
		this.activationTimestamp = order.getActivationTimestamp();
		this.endDate = order.getEndDate();
		this.userId = order.getRequestedUserId();
		this.linkedUserId = order.getLinkedUserId();
		this.status = order.getStatus().name();
		this.optionalJson = order.getToken();
	}
}
