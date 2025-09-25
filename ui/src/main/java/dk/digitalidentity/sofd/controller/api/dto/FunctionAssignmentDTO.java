package dk.digitalidentity.sofd.controller.api.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FunctionAssignmentDTO {

	private long id;
	private LocalDate startDate;
	private LocalDate stopDate;
	private String affiliationUuid;
	private String affiliationPersonName;
	private String affiliationPersonUserId;
	private FunctionDTO function;
	private List<FacetValueDTO> facetValues;
	private String otherAssignments;
}
