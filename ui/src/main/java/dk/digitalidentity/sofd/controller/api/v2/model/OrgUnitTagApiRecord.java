package dk.digitalidentity.sofd.controller.api.v2.model;

import dk.digitalidentity.sofd.dao.model.OrgUnitTag;
import dk.digitalidentity.sofd.dao.model.enums.TagType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class OrgUnitTagApiRecord {
    private String customValue;
    private String tag;
    private TagType tagType;

    public OrgUnitTagApiRecord(OrgUnitTag tag) {
        this.customValue = tag.getTag().isCustomValueEnabled() ? tag.getCustomValue() : null;
        this.tag = tag.getTag().getValue();
        this.tagType = tag.getTag().getTagType();
    }
}
