package dk.digitalidentity.sofd.controller.rest.model;

import javax.validation.constraints.Size;

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
}
