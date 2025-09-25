package dk.digitalidentity.sofd.controller.validation;

import java.text.SimpleDateFormat;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import dk.digitalidentity.sofd.controller.mvc.dto.AffiliationEditDTO;

@Component
public class AffiliationEditDTOValidator implements Validator {

	@Override
	public boolean supports(Class<?> aClass) {
		return (AffiliationEditDTO.class.isAssignableFrom(aClass));
	}

	@Override
	public void validate(Object o, Errors errors) {
		AffiliationEditDTO affiliationDTO = (AffiliationEditDTO) o;

		if (StringUtils.hasLength(affiliationDTO.getStopDate())) {
			try {
				SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
	
				formatter.parse(affiliationDTO.getStopDate());
			}
			catch (Exception ex) {
				errors.rejectValue("stopDate", "mvc.errors.affiliation.stopdate");
			}
		}
		
		if (!StringUtils.hasLength(affiliationDTO.getPositionName())) {
			errors.rejectValue("positionName", "mvc.errors.affiliation.positionname");
		}
	}
}