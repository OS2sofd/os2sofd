package dk.digitalidentity.sofd.controller.rest.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DisableAccountOrderDTO {
    private boolean create;
    private boolean disable;
    private boolean delete;
}
