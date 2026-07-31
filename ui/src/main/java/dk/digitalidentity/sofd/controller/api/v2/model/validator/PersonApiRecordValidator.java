package dk.digitalidentity.sofd.controller.api.v2.model.validator;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import dk.digitalidentity.sofd.controller.api.v2.model.PersonApiRecord;
import dk.digitalidentity.sofd.controller.api.v2.model.UserApiRecord;
import dk.digitalidentity.sofd.service.SupportedUserTypeService;

@Component
public class PersonApiRecordValidator implements Validator {

	@Autowired
	private SupportedUserTypeService supportedUserTypeService;

    @Override
    public boolean supports(Class<?> clazz) {
        return PersonApiRecord.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        PersonApiRecord record = (PersonApiRecord) target;
        
        if (record.getPhones() != null && record.getPhones().stream().anyMatch(o -> !StringUtils.hasLength(o.getMasterId()))) {
            errors.rejectValue("phones","phones.masterId.missing", "all phones must have valid masterId");
        }

        if (record.getUsers() != null && record.getUsers().stream().anyMatch(o -> !StringUtils.hasLength(o.getMasterId()))) {
            errors.rejectValue("users", "users.masterId.missing", "all users must have valid masterId");
        }

        if (record.getUsers() != null && record.getUsers().size() > 0) {
        	Set<String> userTypes = supportedUserTypeService.findAll().stream().map(su -> su.getKey()).collect(Collectors.toSet());
        	
        	for (UserApiRecord userRecord : record.getUsers()) {
        		if (userRecord.getUserType() != null && !userTypes.contains(userRecord.getUserType())) {
                    errors.rejectValue("users", "users.userType.wrong", "unknown userType: '" + userRecord.getUserType() + "'");
        		}
        	}
        }

        if (record.getAffiliations() != null && record.getAffiliations().stream().anyMatch(o -> !StringUtils.hasLength(o.getMasterId()))) {
            errors.rejectValue("affiliations", "affiliations.masterId.missing", "all affiliations must have valid masterId");
        }

        if (record.getRegisteredPostAddress() != null && !StringUtils.hasLength(record.getRegisteredPostAddress().getMasterId())) {
            errors.rejectValue("registeredPostAddress.masterId", "registeredPostAddress.masterId.missing", "registeredPostAddress must have valid masterId");
        }

        if (record.getResidencePostAddress() != null && !StringUtils.hasLength(record.getResidencePostAddress().getMasterId())) {
            errors.rejectValue("residencePostAddress.masterId", "residencePostAddress.masterId.missing", "residencePostAddress must have valid masterId");
        }
    }
}