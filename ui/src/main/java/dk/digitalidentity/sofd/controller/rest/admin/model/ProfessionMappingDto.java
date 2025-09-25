package dk.digitalidentity.sofd.controller.rest.admin.model;

import dk.digitalidentity.sofd.dao.model.enums.ProfessionMatchType;
import dk.digitalidentity.sofd.dao.model.mapping.ProfessionMapping;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProfessionMappingDto {
    private Long id;
    private String matchValue;
    private ProfessionMatchType matchType;

    public ProfessionMappingDto(ProfessionMapping m) {
        this.id = m.getId();
        this.matchValue = m.getMatchValue();
        this.matchType = m.getMatchType();
    }
}
