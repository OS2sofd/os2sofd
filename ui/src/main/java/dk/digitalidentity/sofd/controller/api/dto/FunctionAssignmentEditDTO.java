package dk.digitalidentity.sofd.controller.api.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FunctionAssignmentEditDTO {
	private LocalDate startDate;
	private LocalDate stopDate;
	private List<FacetValueDTO> facetValues;
}
