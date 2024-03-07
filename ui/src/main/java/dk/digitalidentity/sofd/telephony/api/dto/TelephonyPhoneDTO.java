package dk.digitalidentity.sofd.telephony.api.dto;

import java.util.List;

import dk.digitalidentity.sofd.dao.model.enums.PhoneType;
import dk.digitalidentity.sofd.dao.model.enums.Visibility;
import dk.digitalidentity.sofd.telephony.api.TelephonyApiController;
import dk.digitalidentity.sofd.telephony.dao.model.TelephonyPhone;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TelephonyPhoneDTO {
	private long id;
	private String master;
	private String masterId;
	private String phoneNumber;
	private String vendor;
	private String accountNumber;
	private long ean;
	private PhoneType phoneType;
	private Visibility visibility;
	private String functionType;
	private String lastChanged;
	private String personUuid;
	private String personName;
	private List<OrgUnitTelephonyDTO> orgUnits;
	private String subscriptionType;
	private String notes;

	public TelephonyPhoneDTO(TelephonyPhone telephonyPhone) {
		this.id = telephonyPhone.getId();
		this.master = telephonyPhone.getMaster();
		this.masterId = telephonyPhone.getMasterId();
		this.phoneNumber = telephonyPhone.getPhoneNumber();
		this.vendor = telephonyPhone.getVendor();
		this.accountNumber = telephonyPhone.getAccountNumber();
		this.ean = telephonyPhone.getEan();
		this.phoneType = telephonyPhone.getPhoneType();
		this.visibility = telephonyPhone.getVisibility();
		this.functionType = telephonyPhone.getFunctionType() != null ? telephonyPhone.getFunctionType().getName() : "";
		this.lastChanged = TelephonyApiController.sdf.format(telephonyPhone.getLastChanged());
		this.personUuid = telephonyPhone.getPersonUuid();
		this.personName = telephonyPhone.getPersonName();
		this.subscriptionType = telephonyPhone.getSubscriptionType();
		this.notes = telephonyPhone.getNotes();

		if (telephonyPhone.getOrgUnits() != null) {
			this.orgUnits = telephonyPhone.getOrgUnits().stream().map(OrgUnitTelephonyDTO::new).toList();
		}
	}

}
