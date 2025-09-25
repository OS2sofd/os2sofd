package dk.digitalidentity.sofd.controller.api.dto;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import dk.digitalidentity.sofd.dao.model.enums.AffiliationType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateAffiliationApiDTO {
	private String cpr;
	private String firstname;
	private String surname;
	private String orgUnitUuid;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date startDate;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date stopDate;
	private String positionName;
	private AffiliationType affiliationType;
}
