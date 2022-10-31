package dk.digitalidentity.sofd.controller.validation;

import dk.digitalidentity.sofd.controller.mvc.dto.AffiliationDTO;
import dk.digitalidentity.sofd.controller.mvc.dto.CreatePersonDTO;
import dk.digitalidentity.sofd.controller.mvc.dto.PostDTO;
import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.enums.AffiliationType;
import dk.digitalidentity.sofd.service.OrgUnitService;
import dk.digitalidentity.sofd.service.PersonService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

@Component
public class CreatePersonDTOValidator implements Validator {

	@Autowired
	private PersonService personService;

	@Autowired
	private OrgUnitService orgUnitService;

	@Override
	public boolean supports(Class<?> aClass) {
		return (CreatePersonDTO.class.isAssignableFrom(aClass));
	}

	@Override
	public void validate(Object o, Errors errors) {
		CreatePersonDTO person = (CreatePersonDTO) o;

		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "firstname", "mvc.errors.person.firstname.required");
		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "surname", "mvc.errors.person.surname.required");
		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "cpr", "mvc.errors.person.cpr.required");

		if (person.getFirstname().length() < 1) {
			errors.rejectValue("firstname", "mvc.errors.person.firstname.length");
		}

		if (person.getSurname().length() < 1) {
			errors.rejectValue("surname", "mvc.errors.person.surname.length");
		}

		person.setCpr(person.getCpr().replaceAll("[^0-9]", ""));
		if (person.getCpr().length() != 10) {
			errors.rejectValue("cpr", "mvc.errors.person.cpr.length");
		}

		if (person.getUuid() == null) {
			Person existingPerson = personService.findByCpr(person.getCpr());
			if (existingPerson != null) {
				errors.rejectValue("cpr", "mvc.errors.person.exists");
			}
		}

		// if address is filled out, everything is required
		PostDTO registeredAddress = person.getRegisteredPostAddress();
		if (StringUtils.hasLength(registeredAddress.getStreet()) ||
			StringUtils.hasLength(registeredAddress.getPostalCode()) ||
			StringUtils.hasLength(registeredAddress.getCity()) ||
			StringUtils.hasLength(registeredAddress.getCountry())) {

			if (!StringUtils.hasLength(registeredAddress.getStreet())) {
				errors.rejectValue("registeredPostAddress.street", "mvc.errors.person.registeredPostAddress.street");
			}
			
			if (!StringUtils.hasLength(registeredAddress.getPostalCode())) {
				errors.rejectValue("registeredPostAddress.postalCode", "mvc.errors.person.registeredPostAddress.postalCode");
			}
			
			if (!StringUtils.hasLength(registeredAddress.getCity())) {
				errors.rejectValue("registeredPostAddress.city", "mvc.errors.person.registeredPostAddress.city");
			}
			
			// default to "Danmark" for blank
			if (!StringUtils.hasLength(registeredAddress.getCountry())) {
				registeredAddress.setCountry("Danmark");
			}
		}
		
		AffiliationDTO affiliationDTO = person.getAffiliation();
		if (!StringUtils.hasLength(affiliationDTO.getOrgUnitName()) || !StringUtils.hasLength(affiliationDTO.getOrgUnitUuid())) {
			errors.rejectValue("affiliation.orgUnitUuid", "mvc.errors.affiliation.orgunit");
		}
		else {
			OrgUnit orgUnit = orgUnitService.getByUuid(affiliationDTO.getOrgUnitUuid());
			if (orgUnit == null) {
				errors.rejectValue("affiliation.orgUnitUuid", "mvc.errors.affiliation.orgunit");
			}
			
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
			Date stopDate = null;
			if (StringUtils.hasLength(affiliationDTO.getStopDate())) {
				try {
					stopDate = formatter.parse(affiliationDTO.getStopDate());
				}
				catch (ParseException ex) {
					errors.rejectValue("affiliation.stopDate", "mvc.errors.affiliation.stopdate");
				}				
			}

			if (affiliationDTO.getAffiliationType().equals(AffiliationType.EXTERNAL) && stopDate == null) {
				errors.rejectValue("affiliation.stopDate", "mvc.errors.affiliation.stopdate");
			}
		}
		
		if (!StringUtils.hasLength(affiliationDTO.getPositionName())) {
			errors.rejectValue("affiliation.positionName", "mvc.errors.affiliation.positionname");
		}
	}
}
