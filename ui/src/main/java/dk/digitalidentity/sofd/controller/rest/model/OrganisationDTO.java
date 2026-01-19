package dk.digitalidentity.sofd.controller.rest.model;

import javax.validation.constraints.Size;

import dk.digitalidentity.sofd.dao.model.Organisation;
import dk.digitalidentity.sofd.security.SecurityUtil;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrganisationDTO {
    private long id;
    
    @Size(min = 2, max = 64)
    private String shortName;
    
    @Size(min = 2, max = 255)
    private String name;

    private String description;

    private boolean canEdit;

    public OrganisationDTO(){};

    public OrganisationDTO(Organisation organisation) {
        this.id = organisation.getId();
        this.shortName = organisation.getShortName();
        this.name = organisation.getName();
        this.description = organisation.getDescription();
        this.canEdit = SecurityUtil.isLosAdminAuthorizedForOrganisation(organisation);
    }
}
