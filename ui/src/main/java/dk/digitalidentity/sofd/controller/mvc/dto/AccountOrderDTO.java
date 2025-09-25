package dk.digitalidentity.sofd.controller.mvc.dto;

import java.util.Date;

import org.springframework.util.StringUtils;

import dk.digitalidentity.sofd.dao.model.AccountOrder;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.enums.AccountOrderStatus;
import dk.digitalidentity.sofd.dao.model.enums.AccountOrderType;
import dk.digitalidentity.sofd.service.AffiliationService;
import dk.digitalidentity.sofd.service.PersonService;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AccountOrderDTO {
	private long id;

	// which person/affiliation is the account linked to

	private String personUuid;
	private String personName;
	private String employeeId;

	// for custom approval, a token might be send
	private String token;
	
	// details on the actual order
	
	private String userType;
	private AccountOrderType orderType;
	private Date activationTimestamp;
	private Date endDate;
	
	// status on the actual order
	private String message;
	private Date modifiedTimestamp;
	private AccountOrderStatus status;
	private String requestedUserId;
	private String actualUserId;
	private String triggerAffiliation;
	
	public AccountOrderDTO(AccountOrder order, Person person) {
		this.id = order.getId();

		this.personUuid = person.getUuid();
		this.personName = PersonService.getName(person);
		this.employeeId = order.getEmployeeId();
		
		this.userType = order.getUserType();
		this.orderType = order.getOrderType();
		this.activationTimestamp = order.getActivationTimestamp();
		this.endDate = order.getEndDate();

		this.message = order.getMessage();
		this.status = order.getStatus();
		this.modifiedTimestamp = order.getModifiedTimestamp();
		this.requestedUserId = order.getRequestedUserId();
		this.actualUserId = order.getActualUserId();
		if( order.getTriggerAffiliation() != null ) {
			this.triggerAffiliation =
					AffiliationService.getPositionName(order.getTriggerAffiliation()) + " i " +
					order.getTriggerAffiliation().getCalculatedOrgUnit().getEntityName();
		}
	}
	
	public String getUserId() {
		if (StringUtils.hasLength(actualUserId)) {
			return actualUserId;
		}
		
		return requestedUserId;
	}
}
