package dk.digitalidentity.sofd.controller.rest.admin.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FunctionHierarchyNotificationDTO {

	private boolean functionAssignmentExpires;
	private boolean functionAssignmentEmployeeNewManager;
	private Long daysBeforeFunctionAssignmentExpires;
}
