package dk.digitalidentity.sofd.dao.model.validator;

import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.Phone;
import dk.digitalidentity.sofd.dao.model.Post;
import dk.digitalidentity.sofd.service.OrgUnitService;

public class OrgUnitValidator implements Validator {
	private static final String ValidKLEExpression = "^([0-9]{2})$|^([0-9]{2}\\.[0-9]{2})$|^([0-9]{2}\\.[0-9]{2}\\.[0-9]{2})$";

	@Override
	public boolean supports(Class<?> clazz) {
		return OrgUnit.class.equals(clazz);
	}

	// prime indication on address types are validated here, as the validation
	// annotations does not cover this specific case
	@Override
	public void validate(Object target, Errors errors) {
		if (target instanceof OrgUnit) {
			OrgUnit orgUnit = (OrgUnit) target;
			
			try {
				if (orgUnit.getUuid() != null) {
					UUID.fromString(orgUnit.getUuid());
				}
			}
			catch (Exception ex) {
				errors.rejectValue("uuid", "validation.orgunit.uuid.format");
			}

			if (orgUnit.getPhones() != null && orgUnit.getPhones().size() > 0) {
				int primes = 0;
				
				for (Phone phone : OrgUnitService.getPhones(orgUnit)) {
					if (phone.isPrime()) {
						primes++;
					}
				}
				
				if (primes != 1) {
					errors.rejectValue("phones", "validation.orgunit.phones.prime");
				}
			}
			
			if (orgUnit.getPostAddresses() != null && orgUnit.getPostAddresses().size() > 0) {
				int primes = 0;
				
				for (Post post : OrgUnitService.getPosts(orgUnit)) {
					if (post.isPrime()) {
						primes++;
					}
				}
				
				if (primes != 1) {
					errors.rejectValue("postAddresses", "validation.orgunit.postAddresses.prime");
				}
			}

			if (orgUnit.getKlePrimary() != null && orgUnit.getKlePrimary().size() > 0) {
				for (String primaryKle : orgUnit.getKlePrimary().stream().map(k -> k.getKleValue()).collect(Collectors.toList())) {
					if (!primaryKle.matches(ValidKLEExpression)) {
						errors.rejectValue("klePrimary", "validation.orgunit.klePrimary.format");
					}
				}
			}

			if (orgUnit.getKleSecondary() != null && orgUnit.getKleSecondary().size() > 0) {
				for (String secondaryKle : orgUnit.getKleSecondary().stream().map(k -> k.getKleValue()).collect(Collectors.toList())) {
					if (!secondaryKle.matches(ValidKLEExpression)) {
						errors.rejectValue("kleSecondary", "validation.orgunit.kleSecondary.format");
					}
				}
			}
		}
	}
}
