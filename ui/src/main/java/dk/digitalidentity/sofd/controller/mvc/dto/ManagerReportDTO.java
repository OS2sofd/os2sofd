package dk.digitalidentity.sofd.controller.mvc.dto;

import java.util.List;

import dk.digitalidentity.sofd.dao.model.enums.OrgUnitManagerSource;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ManagerReportDTO {
	private String uuid;
	private String name;
	private String orgUnitName;
	private List<String> substitutes;
	private List<String> orgUnitSubstitutes;
	private OrgUnitManagerSource source;
	private boolean inherited;
}
