package dk.digitalidentity.sofd.controller.api.v2.model.validator;

import dk.digitalidentity.sofd.controller.api.v2.model.PersonApiRecord;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class PersonApiRecordValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return PersonApiRecord.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        PersonApiRecord record = (PersonApiRecord) target;

        if( record.getPhones() != null && record.getPhones().stream().anyMatch(o -> !StringUtils.hasLength(o.getMasterId()))) {
            errors.rejectValue("phones","phones.masterId.missing", "all phones must have valid masterId");
        }

        if( record.getUsers() != null && record.getUsers().stream().anyMatch(o -> !StringUtils.hasLength(o.getMasterId()))) {
            errors.rejectValue("users", "users.masterId.missing","all users must have valid masterId");
        }

        if( record.getAffiliations() != null && record.getAffiliations().stream().anyMatch(o -> !StringUtils.hasLength(o.getMasterId()))) {
            errors.rejectValue("affiliations", "affiliations.masterId.missing","all affiliations must have valid masterId");
        }

        if( record.getRegisteredPostAddress() != null && !StringUtils.hasLength(record.getRegisteredPostAddress().getMasterId())) {
            errors.rejectValue("registeredPostAddress.masterId", "registeredPostAddress.masterId.missing","registeredPostAddress must have valid masterId");
        }

        if( record.getResidencePostAddress() != null && !StringUtils.hasLength(record.getResidencePostAddress().getMasterId())) {
            errors.rejectValue("residencePostAddress.masterId", "residencePostAddress.masterId.missing", "residencePostAddress must have valid masterId");
        }
    }
}