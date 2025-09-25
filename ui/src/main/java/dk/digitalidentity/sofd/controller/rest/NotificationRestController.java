package dk.digitalidentity.sofd.controller.rest;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.datatables.mapping.Column;
import org.springframework.data.jpa.datatables.mapping.DataTablesInput;
import org.springframework.data.jpa.datatables.mapping.DataTablesOutput;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.sofd.controller.mvc.datatables.dao.NotificationDatatableDao;
import dk.digitalidentity.sofd.dao.model.Notification;
import dk.digitalidentity.sofd.dao.model.NotificationView;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.User;
import dk.digitalidentity.sofd.dao.model.enums.NotificationType;
import dk.digitalidentity.sofd.security.RequireReadAccess;
import dk.digitalidentity.sofd.security.SecurityUtil;
import dk.digitalidentity.sofd.service.NotificationService;
import dk.digitalidentity.sofd.service.PersonService;
import dk.digitalidentity.sofd.service.SupportedUserTypeService;
import dk.digitalidentity.sofd.service.UserService;

@RequireReadAccess
@RestController
public class NotificationRestController {

	@Autowired
	private NotificationDatatableDao notificationDatatableDao;

	@Autowired
	private NotificationService notificationService;
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private PersonService personService;

	@PostMapping("/rest/notifications/list")
	public DataTablesOutput<NotificationView> list(@Valid @RequestBody DataTablesInput input, BindingResult bindingResult, @RequestHeader("show-inactive") boolean showInactive) {
		if (bindingResult.hasErrors()) {
			DataTablesOutput<NotificationView> error = new DataTablesOutput<>();
			error.setError(bindingResult.toString());

			return error;
		}
		
		// search for active
		for (Column column : input.getColumns()) {
			if ("active".equals(column.getData())) {
				column.getSearch().setValue(showInactive ? "false" : "true");
			}
		}
		
		return notificationDatatableDao.findAll(input);
	}

	@PostMapping("/rest/notifications/changeStatus")
	public ResponseEntity<?> changeStatus(@RequestHeader("id") long id, @RequestHeader("status") boolean active) {
		Notification adminTask = notificationService.findById(id);
		if (adminTask == null) {
			return ResponseEntity.notFound().build();
		}

		String loggedInUserId = SecurityUtil.getUser();
		if (loggedInUserId != null) {
			User user = userService.findByUserIdAndUserType(loggedInUserId, SupportedUserTypeService.getActiveDirectoryUserType());
			if (user != null) {
				Person person = personService.findByUser(user);
				if (person != null) {
					adminTask.setAdminName(PersonService.getName(person) + " (" + user.getUserId() + ")");
					adminTask.setAdminUuid(person.getUuid());
				}
			}
			else {
				adminTask.setAdminName("(" + loggedInUserId + ")");
			}
		}
		
		// BSG: The check below is a bit of a hack, but until we get context-awareness in notifications, we need
		// to delete these to avoid them blocking newer notifications of the same type on the same orgUnit
		if (active == false && (adminTask.getNotificationType().equals(NotificationType.ORGUNIT_WITH_MISSING_RULES) || adminTask.getNotificationType().equals(NotificationType.ORGUNIT_WITH_MISSING_RULES_TITLES))) {
			notificationService.delete(adminTask);
		}
		else {
			adminTask.setActive(active);
			notificationService.save(adminTask);
		}

		return ResponseEntity.ok("");
	}
	
	@PostMapping("/rest/notifications/flipAssign/{id}")
	public ResponseEntity<?> flipAssign(@PathVariable("id") long id, @RequestHeader("confirm") boolean confirm) {
		Notification adminTask = notificationService.findById(id);
		if (adminTask == null) {
			return ResponseEntity.notFound().build();
		}
		
		String userId = SecurityUtil.getUser();
		Person person = personService.getLoggedInPerson();
		if (person == null) {
			return ResponseEntity.badRequest().build();
		}

		if (StringUtils.hasLength(adminTask.getAdminUuid())) {
			if (!confirm || adminTask.getAdminUuid().equals(person.getUuid())) {
				adminTask.setAdminName(null);
				adminTask.setAdminUuid(null);
			}
			else {
				adminTask.setAdminName(PersonService.getName(person) + " (" + userId + ")");
				adminTask.setAdminUuid(person.getUuid());
			}
		}
		else {
			adminTask.setAdminName(PersonService.getName(person) + " (" + userId + ")");
			adminTask.setAdminUuid(person.getUuid());
		}
		
		notificationService.save(adminTask);
		
		return ResponseEntity.ok("");
	}
}
