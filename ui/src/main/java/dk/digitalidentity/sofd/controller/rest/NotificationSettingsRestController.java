package dk.digitalidentity.sofd.controller.rest;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.sofd.controller.rest.model.NotificationSettingsDTO;
import dk.digitalidentity.sofd.dao.model.enums.EntityType;
import dk.digitalidentity.sofd.dao.model.enums.EventType;
import dk.digitalidentity.sofd.dao.model.enums.NotificationType;
import dk.digitalidentity.sofd.log.AuditLogger;
import dk.digitalidentity.sofd.security.RequireControllerWriteAccess;
import dk.digitalidentity.sofd.service.NotificationService;
import dk.digitalidentity.sofd.service.SettingService;

@RequireControllerWriteAccess
@RestController
public class NotificationSettingsRestController {

    @Autowired
    private AuditLogger auditLogger;
	
	@Autowired
    private SettingService settingService;

    @Autowired
    private NotificationService notificationService;

    @PostMapping(value = "/rest/admin/notifications/settings")
    @ResponseBody
    public HttpEntity<String> saveSettings(@RequestBody NotificationSettingsDTO settingsDTO) {
        for (Map.Entry<NotificationType, Boolean> entry : settingsDTO.getNotificationTypes().entrySet()) {
        	//checks if notification settings changed, and if so, then it changes it and writes an auditlog 
        	if(settingService.isNotificationTypeEnabled(entry.getKey()) != entry.getValue()) {
        		settingService.setNotificationTypeEnabled(entry.getKey(), entry.getValue());
        		String message = "Adviset " + entry.getKey() + " slået " + (entry.getValue() ? "til" : "fra");
        		auditLogger.log("-", EntityType.SETTING, EventType.SAVE, entry.getKey().toString(), message);
        	}
            
        }

        // delete already created of disabled types
        if (settingsDTO.isDeleteAlreadyCreated()) {
            for (NotificationType type : NotificationType.values()) {
                if (!settingService.isNotificationTypeEnabled(type)) {
                    notificationService.deleteAllByNotificationType(type);
                }
            }
        }

        settingService.setUserInactivePeriod(settingsDTO.getUserInactivePeriod());

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
