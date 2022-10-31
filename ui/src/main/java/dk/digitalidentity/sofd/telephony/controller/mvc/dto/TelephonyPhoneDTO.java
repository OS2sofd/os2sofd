package dk.digitalidentity.sofd.telephony.controller.mvc.dto;

import java.util.Date;

import dk.digitalidentity.sofd.dao.model.enums.PhoneType;
import dk.digitalidentity.sofd.dao.model.enums.Visibility;
import dk.digitalidentity.sofd.telephony.dao.model.TelephonyPhone;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TelephonyPhoneDTO {
	private long id;
	private String master;
	private String phoneNumber;
	private String vendor;
	private String accountNumber;
	private long ean;
	private PhoneType phoneType;
	private Visibility visibility;
	private long functionType;
	private Date lastChanged;
	private String subscriptionType;
	private String notes;

	public TelephonyPhoneDTO(TelephonyPhone phone) {
		this.id = phone.getId();
		this.master = phone.getMaster();
		this.lastChanged = phone.getLastChanged();
		this.phoneNumber = phone.getPhoneNumber();
		this.vendor = phone.getVendor();
		this.accountNumber = phone.getAccountNumber();
		this.ean = phone.getEan();
		this.phoneType = phone.getPhoneType();
		this.visibility = phone.getVisibility();
		this.functionType = phone.getFunctionType() != null ? phone.getFunctionType().getId() : 0;
		this.subscriptionType = phone.getSubscriptionType();
		this.notes = phone.getNotes();
	}
}
