package dk.digitalidentity.sofd.controller.mvc.dto;

import java.util.List;

import org.springframework.data.util.Pair;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmployeeDTO {
	private String name;
	private String uuid;
	private String positionName;
	private String employmentTermsText;
	private String vendor;
	private String startDate;
	private String stopDate;
	private boolean leave;
	private boolean forceStop;
	private boolean disableAccountOrdersCreate;
	private boolean disableAccountOrdersDisable;
	private boolean disableAccountOrdersDelete;
	private boolean fictiveCpr;
	private List<Pair<String, Boolean>> accounts;
}
