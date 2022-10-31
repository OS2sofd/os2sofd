package dk.digitalidentity.sofd.controller.api.dto;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FunctionAssignmentDTO {

	private long id;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date startDate;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date stopDate;
	private String affiliationUuid;
	private String affiliationPersonName;
	private String affiliationPersonUserId;
	private FunctionDTO function;
	private List<FacetValueDTO> facetValues;
}
