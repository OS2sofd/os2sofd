package dk.digitalidentity.sofd.dao.model.enums;

import lombok.Getter;

@Getter
public enum ManualNotificationFrequency {
    ONCE("html.enum.manualNotificationFrequency.once"),
    DAILY("html.enum.manualNotificationFrequency.daily"),
    MONTHLY("html.enum.manualNotificationFrequency.monthly")
    ;

    private String message;

    private ManualNotificationFrequency(String message) { this.message = message; }
}
