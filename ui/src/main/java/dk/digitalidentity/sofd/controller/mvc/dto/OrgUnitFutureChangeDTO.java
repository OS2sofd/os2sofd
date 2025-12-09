package dk.digitalidentity.sofd.controller.mvc.dto;

import java.util.Date;

import dk.digitalidentity.sofd.dao.model.OrgUnitFutureChange;
import dk.digitalidentity.sofd.dao.model.enums.OrgUnitAttribute;
import dk.digitalidentity.sofd.dao.model.enums.OrgUnitChangeType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OrgUnitFutureChangeDTO {
	
    private long id;
    private Date changeDate;
    private String orgunitUuid;
    private String orgunitName;
    private String parentName;
    private OrgUnitChangeType changeType;
    private String attributeValue;
    private OrgUnitAttribute attributeField;
	private String details;
	private Long tagId;
	private String tagValue;

	public OrgUnitFutureChangeDTO(OrgUnitFutureChange source) {
		this.id = source.getId();
		this.changeDate = source.getChangeDate();
		this.orgunitName = source.getOrgunitName();
		this.orgunitUuid = source.getOrgunitUuid();
		this.parentName = source.getParentName();
		this.attributeField = source.getAttributeField();
		this.attributeValue = source.getAttributeValue();
		this.changeType = source.getChangeType();
		this.tagId = source.getTagId();
		this.tagValue = source.getTagValue();
	}
}
