package dk.digitalidentity.sofd.dao.model.enums;

import lombok.Getter;

@Getter
public enum ManualNotificationFrequency {
    ONCE("html.enum.manualNotificationFrequency.once", "html.enum.manualNotificationFrequency.once"),
    DAILY("html.enum.manualNotificationFrequency.daily", "html.enum.manualNotificationFrequency.daily.unit"),
    MONTHLY("html.enum.manualNotificationFrequency.monthly", "html.enum.manualNotificationFrequency.monthly.unit")
    ;

    private String message;
    private String unit;

    private ManualNotificationFrequency(String message, String unit) { this.message = message; this.unit = unit; }
}
