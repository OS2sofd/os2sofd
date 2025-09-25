package dk.digitalidentity.sofd.controller.mvc.dto;

import java.util.Date;

import dk.digitalidentity.sofd.controller.mvc.dto.enums.PendingEmailType;
import dk.digitalidentity.sofd.dao.model.EmailQueue;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailQueueDTO {
	private long id;
	private String recipient;
	private PendingEmailType type;
	private String title;
	private Date deliveryTts;

	public EmailQueueDTO(EmailQueue pendingEmail) {
		this.id = pendingEmail.getId();
		this.recipient = pendingEmail.getRecipient();
		this.type = (pendingEmail.getCpr() != null ? PendingEmailType.DIGITALPOST : PendingEmailType.EMAIL);
		this.title = pendingEmail.getTitle();
		this.deliveryTts = pendingEmail.getDeliveryTts();
	}
}
