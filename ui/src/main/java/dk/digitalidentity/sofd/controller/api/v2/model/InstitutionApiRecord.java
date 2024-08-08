package dk.digitalidentity.sofd.controller.api.v2.model;

import dk.digitalidentity.sofd.dao.model.Institution;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class InstitutionApiRecord {
	private String name;
	private String institutionNumber;

	// read-only
	private String uuid;

	public InstitutionApiRecord(Institution institution) {
		this.name = institution.getName();
		this.institutionNumber = institution.getInstitutionNumber();
		this.uuid = institution.getUuid();
	}
}