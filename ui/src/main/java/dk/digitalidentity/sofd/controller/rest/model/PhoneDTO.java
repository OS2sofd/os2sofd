package dk.digitalidentity.sofd.controller.rest.model;

import dk.digitalidentity.sofd.dao.model.Phone;
import dk.digitalidentity.sofd.dao.model.enums.PhoneType;
import dk.digitalidentity.sofd.dao.model.enums.Visibility;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PhoneDTO {
	private long id;
	private String phoneNumber;
	private PhoneType phoneType;
	private String notes;
	private Visibility visibility;
	private long functionType;
	private String functionTypeName;
	private boolean prime;
	private boolean typePrime;
	private String master;
	private String masterId;
	
	public PhoneDTO(Phone phone) {
		this.id = phone.getId();
		this.phoneNumber = phone.getPhoneNumber();
		this.phoneType = phone.getPhoneType();
		this.notes = phone.getNotes();
		this.visibility = phone.getVisibility();
		this.functionType = phone.getFunctionType() != null ? phone.getFunctionType().getId() : 0;
		this.functionTypeName = phone.getFunctionType() != null ? phone.getFunctionType().getName() : null; 
		this.prime = phone.isPrime();
		this.typePrime = phone.isTypePrime();
		this.master = phone.getMaster();
		this.masterId = phone.getMasterId();
	}
}
