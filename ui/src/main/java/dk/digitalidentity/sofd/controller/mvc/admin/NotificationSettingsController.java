package dk.digitalidentity.sofd.controller.mvc.admin;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import dk.digitalidentity.sofd.controller.rest.model.ManualNotificationRestDTO;
import dk.digitalidentity.sofd.dao.model.ManualNotification;
import dk.digitalidentity.sofd.service.ManualNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import dk.digitalidentity.sofd.dao.model.enums.CustomerSetting;
import dk.digitalidentity.sofd.dao.model.enums.NotificationType;
import dk.digitalidentity.sofd.security.RequireAdminAccess;
import dk.digitalidentity.sofd.service.SettingService;
import org.springframework.web.bind.annotation.PathVariable;

@RequireAdminAccess
@Controller
public class NotificationSettingsController {

    @Autowired
    private ManualNotificationService manualNotificationService;

	@Autowired
	private SettingService settingService;

	@GetMapping("/ui/admin/notifications/settings")
	public String editNotificationSettings(Model model) {
		// For each NotificationType check if it is enabled and create map
		Map<NotificationType, Boolean> notificationEnabledMap = Arrays
				.stream(NotificationType.values())
				.filter( notificationType -> notificationType.isVisibleInUI())
				.collect(Collectors.toMap(notificationType -> notificationType, notificationType -> settingService.isNotificationTypeEnabled(notificationType)));

		model.addAttribute("settings", notificationEnabledMap);
		model.addAttribute("userInactivePeriod", settingService.getLongValueByKey(CustomerSetting.USER_INACTIVE_PERIOD));

		return "admin/notification/settings";
	}

    @GetMapping("/ui/admin/notifications/manual")
    public String listManualNotifications(Model model) {
        model.addAttribute("manuals", manualNotificationService.findAll().stream().map(manualNotificationService::toDTO).toList()); //uses dto focused on frontend readability
        return "admin/notification/listManuals";
    }

    @GetMapping("/ui/admin/notifications/manual/{manualId}")
    public String newManualNotification(@PathVariable long manualId, Model model) {
        ManualNotification manualNotification = manualNotificationService.findById(manualId).orElse(new ManualNotification());
        model.addAttribute("manual", ManualNotificationRestDTO.toDTO(manualNotification)); //this focus data transference
        return "admin/notification/editManual";
    }
}
