package dk.digitalidentity.sofd.controller.mvc.dto;

import java.util.List;

import dk.digitalidentity.sofd.dao.model.User;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmployeeWithUsersDTO {
	private String name;
	private String uuid;
	private String positionName;
	private String startDate;
	private String stopDate;
	private String orgUnitName;
	private String manager;
	private List<User> users;
	private String employmentTerms;
	private String orgUnitUuid;
	private String internalReference;
	private String managerUsername;
	private String managerEmployeeNumber;
	private String employeeNumber;
	private boolean primeAffiliation;
	private boolean isOnLeave;

}
