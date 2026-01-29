package dk.digitalidentity.sofd.controller.api.dto;

import dk.digitalidentity.sofd.dao.model.SupportedUserType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SupportedUserTypeDTO {

    private String key;
    private String name;
    private boolean canOrder;
    private boolean singleUserMode;
    private long daysBeforeToCreate;
    private long daysToDeactivate;
    private long daysToDelete;
    private String dependsOnKey;
    private long minutesDelay;
    private boolean deactivateEnabled;
    private boolean deleteEnabled;
    private boolean createEnabled;

    public SupportedUserTypeDTO(SupportedUserType supportedUserType) {
        this.key = supportedUserType.getKey();
        this.name = supportedUserType.getName();
        this.canOrder = supportedUserType.isCanOrder();
        this.singleUserMode = supportedUserType.isSingleUserMode();
        this.daysBeforeToCreate = supportedUserType.getDaysBeforeToCreate();
        this.daysToDeactivate = supportedUserType.getDaysToDeactivate();
        this.daysToDelete = supportedUserType.getDaysToDelete();
        this.dependsOnKey = supportedUserType.getDependsOn() != null ? supportedUserType.getDependsOn().getKey() : null;
        this.minutesDelay = supportedUserType.getMinutesDelay();
        this.deactivateEnabled = supportedUserType.isDeactivateEnabled();
        this.deleteEnabled = supportedUserType.isDeleteEnabled();
        this.createEnabled = supportedUserType.isCreateEnabled();
    }
}