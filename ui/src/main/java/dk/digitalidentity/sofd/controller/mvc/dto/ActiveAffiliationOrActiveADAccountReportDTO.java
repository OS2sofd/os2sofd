package dk.digitalidentity.sofd.controller.mvc.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ActiveAffiliationOrActiveADAccountReportDTO {
	private String uuid;
	private String name;
	private List<String> users;
	private List<String> affiliations;
}
