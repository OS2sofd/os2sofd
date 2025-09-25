package dk.digitalidentity.sofd.controller.api.v2.model;

import javax.validation.constraints.NotNull;

import dk.digitalidentity.sofd.dao.model.Phone;
import dk.digitalidentity.sofd.dao.model.enums.PhoneType;
import dk.digitalidentity.sofd.dao.model.enums.Visibility;
import dk.digitalidentity.sofd.service.FunctionTypeService;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class PhoneApiRecord extends BaseRecord {

	// primary key
	
	@NotNull
	private String master;
	
	@NotNull
	private String masterId;

	// read/write fields
	
	@NotNull
	private String phoneNumber;
	private String phoneType;
	private String visibility;
	private long functionTypeId;

	// readonly
	private String functionTypeName;
	private boolean prime;
	private boolean typePrime;
	
	public PhoneApiRecord(Phone phone) {
		this.master = phone.getMaster();
		this.masterId = phone.getMasterId();
		this.phoneNumber = phone.getPhoneNumber();
		this.phoneType = (phone.getPhoneType() != null) ? phone.getPhoneType().toString() : null;
		this.visibility = (phone.getVisibility() != null) ? phone.getVisibility().toString() : null;
		this.functionTypeId = (phone.getFunctionType() != null) ? phone.getFunctionType().getId() : 0;
		this.functionTypeName = (phone.getFunctionType() != null) ? phone.getFunctionType().getName() : null;
		
		this.prime = phone.isPrime();
		this.typePrime = phone.isTypePrime();
	}

	public Phone toPhone() {
		Phone phone = new Phone();
		phone.setFunctionType((functionTypeId != 0) ? FunctionTypeService.getInstance().findById(functionTypeId) : null);
		phone.setMaster(master);
		phone.setMasterId(masterId);
		phone.setPhoneNumber(phoneNumber);
		phone.setPhoneType((phoneType != null) ? PhoneType.valueOf(phoneType) : null);
		phone.setVisibility((visibility != null) ? Visibility.valueOf(visibility) : null);
		
		return phone;
	}
}
