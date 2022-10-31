package dk.digitalidentity.sofd.dao.model.validator;

import java.util.UUID;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.Phone;
import dk.digitalidentity.sofd.dao.model.User;
import dk.digitalidentity.sofd.dao.model.mapping.AffiliationPrimaryKleMapping;
import dk.digitalidentity.sofd.dao.model.mapping.AffiliationSecondaryKleMapping;
import dk.digitalidentity.sofd.service.PersonService;
import dk.digitalidentity.sofd.service.SupportedUserTypeService;

public class PersonValidator implements Validator {
	private static final String ValidKLEExpression = "^([0-9]{2})$|^([0-9]{2}\\.[0-9]{2})$|^([0-9]{2}\\.[0-9]{2}\\.[0-9]{2})$";
	private SupportedUserTypeService userTypeService;
	
	public PersonValidator(SupportedUserTypeService userTypeService) {
		this.userTypeService = userTypeService;
	}

	@Override
	public boolean supports(Class<?> clazz) {
		return Person.class.equals(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		if (target instanceof Person) {
			Person person = (Person) target;

			try {
				if (person.getUuid() != null) {
					UUID.fromString(person.getUuid());
				}
			}
			catch (Exception ex) {
				errors.rejectValue("uuid", "validation.person.uuid.format");
			}

			if (person.getRegisteredPostAddress() != null || person.getResidencePostAddress() != null) {
				int primes = 0;
				
				if (person.getRegisteredPostAddress() != null && person.getRegisteredPostAddress().isPrime()) {
					primes++;
				}
				
				if (person.getResidencePostAddress() != null && person.getResidencePostAddress().isPrime()) {
					primes++;
				}
				
				if (primes != 1) {
					errors.rejectValue("registeredPostAddress", "validation.person.registeredPostAddress.prime");
				}
			}

			if (person.getPhones() != null && person.getPhones().size() > 0) {
				int primes = 0;
				
				for (Phone phone : PersonService.getPhones(person)) {
					if (phone.isPrime()) {
						primes++;
					}
				}
				
				if (primes != 1) {
					errors.rejectValue("phones", "validation.person.phones.prime");
				}
			}

			if (person.getUsers() != null && person.getUsers().size() > 0) {
				for (User user : PersonService.getUsers(person)) {
					if (!userTypeService.isValidUserType(user.getUserType())) {
						errors.rejectValue("users", "validation.user.usertype.invalid");
					}
				}
			}

			if (person.getAffiliations() != null && person.getAffiliations().size() > 0) {
				for (Affiliation affiliation : person.getAffiliations()) {
					if (affiliation.getKlePrimary() != null && affiliation.getKlePrimary().size() > 0) {
						for (AffiliationPrimaryKleMapping primaryKle : affiliation.getKlePrimary()) {
							if (!primaryKle.getKleValue().matches(ValidKLEExpression)) {
								errors.rejectValue("affiliations", "validation.affiliation.klePrimary.format");
							}
						}
					}

					if (affiliation.getKleSecondary() != null && affiliation.getKleSecondary().size() > 0) {
						for (AffiliationSecondaryKleMapping secondaryKle : affiliation.getKleSecondary()) {
							if (!secondaryKle.getKleValue().matches(ValidKLEExpression)) {
								errors.rejectValue("affiliations", "validation.affiliation.kleSecondary.format");
							}
						}
					}
				}
			}
		}
	}
}
