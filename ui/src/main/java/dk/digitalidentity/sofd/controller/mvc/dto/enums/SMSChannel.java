package dk.digitalidentity.sofd.controller.mvc.dto.enums;

import lombok.Getter;


@Getter
public enum SMSChannel {
    SMS("SMS",true),
    EMAIL("Email",false),
    EMAIL_OR_SMS("Email (eller SMS, hvis ingen email)",true);

    private String title;
    private boolean smsModuleRequired;

    SMSChannel(String title, boolean smsModuleRequired) {
        this.title = title;
        this.smsModuleRequired = smsModuleRequired;
    }
}