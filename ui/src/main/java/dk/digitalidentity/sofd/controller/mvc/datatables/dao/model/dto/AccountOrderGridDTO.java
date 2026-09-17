package dk.digitalidentity.sofd.controller.mvc.datatables.dao.model.dto;

import java.util.Locale;

import org.springframework.context.MessageSource;

import dk.digitalidentity.sofd.controller.mvc.datatables.dao.model.AccountOrderView;
import dk.digitalidentity.sofd.dao.model.enums.AccountOrderType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccountOrderGridDTO {
	private long id;
	private String activationTimestamp;
	private String personUuid;
	private String personName;
	private String triggerAffiliation;
	private String userId;
	private String orderType;
	private String userType;
	private String status;

	public AccountOrderGridDTO(AccountOrderView order, String prettyUserType, MessageSource messageSource, Locale locale) {
		this.id = order.getId();
		this.activationTimestamp = order.getActivationTimestamp();
		this.personUuid = order.getPersonUuid();
		this.personName = order.getPersonName();
		this.triggerAffiliation = order.getTriggerAffiliation();
		this.userId = order.getUserId();
		this.userType = prettyUserType;
		this.status = messageSource.getMessage(order.getStatus().getMessageId(), null, locale);

		// an EXPIRE order without an end-date is really a "remove the expire-date" order
		if (AccountOrderType.EXPIRE.equals(order.getOrderType()) && order.getEndDate() == null) {
			this.orderType = messageSource.getMessage("html.enum.accountOrderStatus.expireReverse", null, locale);
		}
		else {
			this.orderType = messageSource.getMessage(order.getOrderType().getMessageId(), null, locale);
		}
	}
}
