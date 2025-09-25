package dk.digitalidentity.sofd.service.model;

import dk.digitalidentity.sofd.dao.model.OrgUnit;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OUTreeForm {
	private String id;
	private String parent;
	private String text;

	public OUTreeForm(OrgUnit ou) {
		this.text = ou.getName();
		this.id = ou.getUuid();
		this.parent = (ou.getParent() != null) ? ou.getParent().getUuid() : "#";
	}

	public OUTreeForm() {}
}
