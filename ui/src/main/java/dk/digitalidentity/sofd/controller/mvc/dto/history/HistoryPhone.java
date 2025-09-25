package dk.digitalidentity.sofd.controller.mvc.dto.history;

import dk.digitalidentity.sofd.dao.model.Phone;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HistoryPhone {
	private String master;
	private String masterId;
	private String phoneNumber;
	private String phoneType;
	private String visibility;
	private String functionType;
	private boolean prime;
	private boolean typePrime;
	
	public HistoryPhone(Phone phone) {
		this.master = phone.getMaster();
		this.masterId = phone.getMaster();
		this.phoneNumber = phone.getPhoneNumber();
		this.phoneType = (phone.getPhoneType() != null) ? phone.getPhoneType().toString() : null;
		this.visibility = (phone.getVisibility() != null) ? phone.getVisibility().toString() : null;
		this.functionType = (phone.getFunctionType() != null) ? phone.getFunctionType().getName() : null;
		this.prime = phone.isPrime();
		this.typePrime = phone.isTypePrime();
	}
}
