package dk.digitalidentity.sofd.controller.api.v2.model;

import javax.validation.constraints.NotNull;

import dk.digitalidentity.sofd.dao.model.Ean;
import dk.digitalidentity.sofd.dao.model.OrgUnit;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class EanApiRecord extends BaseRecord {

	@NotNull
	private String master;
	
	private long number;
	
	//readonly
	private boolean prime;

	public EanApiRecord(Ean ean) {
		this.master = ean.getMaster();
		this.number = ean.getNumber();
		this.prime = ean.isPrime();
	}

	public Ean toEan(OrgUnit orgUnit) {
		Ean eanEntity = new Ean();
		eanEntity.setMaster(this.master);
		eanEntity.setNumber(this.number);
		eanEntity.setOrgUnit(orgUnit);
		
		return eanEntity;
	}

}
