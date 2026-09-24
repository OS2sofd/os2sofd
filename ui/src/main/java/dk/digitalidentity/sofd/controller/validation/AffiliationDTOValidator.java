package dk.digitalidentity.sofd.controller.validation;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import dk.digitalidentity.sofd.controller.mvc.dto.AffiliationDTO;
import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.service.AffiliationDurationService;
import dk.digitalidentity.sofd.service.OrgUnitService;

@Component
public class AffiliationDTOValidator implements Validator {

	@Autowired
	private OrgUnitService orgUnitService;

	@Autowired
	private AffiliationDurationService affiliationDurationService;

	@Override
	public boolean supports(Class<?> aClass) {
		return (AffiliationDTO.class.isAssignableFrom(aClass));
	}

	@Override
	public void validate(Object o, Errors errors) {
		AffiliationDTO affiliationDTO = (AffiliationDTO) o;

		if (!StringUtils.hasLength(affiliationDTO.getOrgUnitName()) || !StringUtils.hasLength(affiliationDTO.getOrgUnitUuid())) {
			errors.rejectValue("orgUnitUuid", "mvc.errors.affiliation.orgunit");
		}
		else {
			OrgUnit orgUnit = orgUnitService.getByUuid(affiliationDTO.getOrgUnitUuid());
			if (orgUnit == null) {
				errors.rejectValue("orgUnitUuid", "mvc.errors.affiliation.orgunit");
			}
			else {
				String durationError = affiliationDurationService.validate(orgUnit, affiliationDTO.getStartDate(), affiliationDTO.getStopDate());
				if (durationError != null) {
					errors.rejectValue("stopDate", "mvc.errors.affiliation.stopdate.duration", durationError);
				}
			}
		}

		if (StringUtils.hasLength(affiliationDTO.getStartDate()) && !isValidDate(affiliationDTO.getStartDate())) {
			errors.rejectValue("startDate", "mvc.errors.affiliation.startdate");
		}

		if (StringUtils.hasLength(affiliationDTO.getStopDate()) && !isValidDate(affiliationDTO.getStopDate())) {
			errors.rejectValue("stopDate", "mvc.errors.affiliation.stopdate");
		}
		
		if (!StringUtils.hasLength(affiliationDTO.getPositionName())) {
			errors.rejectValue("positionName", "mvc.errors.affiliation.positionname");
		}
	}

	private static boolean isValidDate(String value) {
		try {
			new SimpleDateFormat("yyyy-MM-dd").parse(value);
			return true;
		}
		catch (ParseException ex) {
			return false;
		}
	}
}