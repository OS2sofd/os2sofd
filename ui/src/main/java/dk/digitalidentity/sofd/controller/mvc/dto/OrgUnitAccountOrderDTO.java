package dk.digitalidentity.sofd.controller.mvc.dto;

import java.util.ArrayList;
import java.util.List;

import dk.digitalidentity.sofd.dao.model.OrgUnitAccountOrder;
import dk.digitalidentity.sofd.dao.model.OrgUnitAccountOrderType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrgUnitAccountOrderDTO {
	private String orgunitUuid;
	private List<OrgUnitAccountOrderTypeDTO> types;
	
	public OrgUnitAccountOrderDTO(OrgUnitAccountOrder orig) {
		this.orgunitUuid = orig.getOrgunitUuid();
		this.types = new ArrayList<OrgUnitAccountOrderTypeDTO>();
		
		if (orig.getTypes() != null) {
			for (OrgUnitAccountOrderType origType : orig.getTypes()) {
				this.types.add(new OrgUnitAccountOrderTypeDTO(origType));
			}
		}
	}
}
