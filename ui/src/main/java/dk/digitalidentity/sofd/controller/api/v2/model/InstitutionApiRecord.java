package dk.digitalidentity.sofd.controller.api.v2.model;

import dk.digitalidentity.sofd.dao.model.Institution;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// read-only API class
@Getter
@Setter
@ToString
public class InstitutionApiRecord {
	private String name;
	private String institutionNumber;
	private String uuid;

	public InstitutionApiRecord(Institution institution) {
		this.name = institution.getName();
		this.institutionNumber = institution.getInstitutionNumber();
		this.uuid = institution.getUuid();
	}
}
