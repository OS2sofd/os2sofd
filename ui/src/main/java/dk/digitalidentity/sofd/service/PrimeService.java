package dk.digitalidentity.sofd.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.Email;
import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.Phone;
import dk.digitalidentity.sofd.dao.model.Post;
import dk.digitalidentity.sofd.dao.model.User;
import dk.digitalidentity.sofd.dao.model.enums.PhoneType;

@Service
public class PrimeService {

	@Autowired
	private SofdConfiguration configuration;

	public boolean setPrimeUser(List<User> users) {
		long primeCount = 0;

		for (User user : users) {
			// if for some reason the user account has been disabled, it can no longer be prime
			if (user.isDisabled() == true) {
				user.setPrime(false);
			}

			if (user.isPrime()) {
				primeCount++;
			}
		}

		// if our integration does not make sure there is exactly ONE prime of each type,
		// then we flag the first non-disabled one of them as prime to make sure data is consistent
		if (primeCount != 1) {
			Optional<User> selectedPrimeUser = users.stream().filter(u -> u.isDisabled() == false).findFirst();
			if (selectedPrimeUser.isPresent()) {
				selectedPrimeUser.get().setPrime(true);

				users.stream().filter(u -> u.getId() != selectedPrimeUser.get().getId()).forEach(u -> u.setPrime(false));
				return true;
			}
			else {
				// if no prime can be found, NO user can be prime
				users.stream().forEach(u -> u.setPrime(false));
			}
		}
		
		return false;
	}

	public boolean setPrimeAffilation(Person person) {
		if (person.getAffiliations() == null || person.getAffiliations().size() == 0) {
			return false;
		}
		
		// rule 0: filter out affiliations that are deleted or expired
		List<Affiliation> activeAffiliations = AffiliationService.onlyActiveAffiliations(person.getAffiliations());
		
		// special case: if there are no active affiliations, but we have future affiliations, we should add those
		// so we pick one of them as the prime affiliation (otherwise our delete job will set the deleted flag on the user)
		if (activeAffiliations.size() == 0) {
			for (Affiliation affiliation : person.getAffiliations()) {
				if (AffiliationService.notActiveYet(affiliation, 0)) {
					activeAffiliations.add(affiliation);
				}
			}
		}

		if (activeAffiliations.size() == 0) {
			boolean changes = false;
			
			// any remaining affiliations that are prime, are now flagged as non-primes
			for (Affiliation a : person.getAffiliations()) {
				if (a.isPrime()) {
					a.setPrime(false);
					changes = true;
				}
			}					
			
			return changes;
		}

		// Use user-selected affiliation if provided (affiliation with a start-date in the future are allowed here)
		List<Affiliation> notStoppedAffiliations = AffiliationService.notStoppedAffiliations(person.getAffiliations());
		Optional<Affiliation> selectedPrime = notStoppedAffiliations.stream().filter(a -> a.isSelectedPrime()).findFirst();
		if (selectedPrime.isPresent()) {
			Affiliation aPrime = selectedPrime.get();
			boolean change = false;

			for (Affiliation a : person.getAffiliations()) {
				if (a.getId() == aPrime.getId()) {
					if (!a.isPrime()) {
						a.setPrime(true);
						change = true;
					}
				}
				else {
					if (a.isPrime()) {
						a.setPrime(false);
						change = true;
					}
				}
			}

			if (change) {
				return true;
			}
			
			return false;
		}

		// rule 1: prefer affiliations from prime affiliation master (default OPUS)
		List<Affiliation> opusAffiliations = activeAffiliations.stream()
				  .filter(a -> a.getMaster().equals(configuration.getModules().getLos().getPrimeAffiliationMaster()))
				  .collect(Collectors.toList());

		if (opusAffiliations.size() > 0) {
			// rule 2: LOWEST employmentTerms, and if equal, fallback to HIGHEST working hours, and finally LOWEST employeeId
			Optional<Affiliation> affiliation = opusAffiliations.stream().max((a1, a2)
					-> (a1.getEmploymentTerms().equals(a2.getEmploymentTerms())
						? (a1.getWorkingHoursNumerator() == a2.getWorkingHoursNumerator()
							? a2.getEmployeeId().compareTo(a1.getEmployeeId())
							: a1.getWorkingHoursNumerator().compareTo(a2.getWorkingHoursNumerator()))
						: a2.getEmploymentTerms().compareTo(a1.getEmploymentTerms())));
					
			if (affiliation.isPresent()) {
				Affiliation aPrime = affiliation.get();
				boolean change = false;
				
				for (Affiliation a : person.getAffiliations()) {
					if (a == aPrime) {
						if (!a.isPrime()) {
							a.setPrime(true);
							change = true;
						}
					}
					else {
						if (a.isPrime()) {
							a.setPrime(false);
							change = true;
						}
					}
				}

				if (change) {
					return true;
				}
			}			
		}
		else {
			// rule 3: fall back to a random prime affiliation if number of active primes is not exactly 1
			if (activeAffiliations.stream().filter(a -> a.isPrime()).count() != 1) {
				Optional<Affiliation> aPrime = activeAffiliations.stream().sorted(Comparator.comparing(Affiliation::getUuid)).findFirst();
				boolean change = false;

				for (Affiliation a : person.getAffiliations()) {
					if (a == aPrime.get()) {
						if (!a.isPrime()) {
							a.setPrime(true);
							change = true;
						}
					}
					else {
						if (a.isPrime()) {
							a.setPrime(false);
							change = true;
						}
					}
				}

				if (change) {
					return true;
				}
			}
		}

		return false;
	}

	public void setPrimePost(Person person) {
		if (person.getRegisteredPostAddress() != null) {
			person.getRegisteredPostAddress().setPrime(true);
			
			if (person.getResidencePostAddress() != null) {
				person.getResidencePostAddress().setPrime(false);
			}
		}
		else if (person.getResidencePostAddress() != null) {
			person.getResidencePostAddress().setPrime(true);
		}
	}
	
	public void setPrimePhone(Person person) {
		if (person.getPhones() == null || person.getPhones().size() == 0) {
			return;
		}

		for (PhoneType phoneType : PhoneType.values()) {
			List<Phone> phonesOfType = PersonService.getPhones(person).stream().filter(p -> p.getPhoneType().equals(phoneType)).collect(Collectors.toList());
			
			if (phonesOfType.size() == 0) {
				continue;
			}

			int typePrimeCount = 0;
			for (Phone phone : phonesOfType) {
				if (phone.isTypePrime()) {
					typePrimeCount++;
				}
			}

			if (typePrimeCount != 1) {
				phonesOfType.forEach(p -> p.setTypePrime(false));
				phonesOfType.get(0).setTypePrime(true);
			}
		}
		
		int primes = 0;

		for (Phone phone : PersonService.getPhones(person)) {
			if (phone.isPrime()) {
				primes++;
			}
		}
			
		if (primes != 1) {
			PersonService.getPhones(person).forEach(p -> p.setPrime(false));

			for (Phone phone : PersonService.getPhones(person)) {
				if (phone.isTypePrime()) {
					phone.setPrime(true);
					break;
				}
			}
		}
	}

	public void setPrimePhone(OrgUnit orgUnit) {
		if (orgUnit.getPhones() == null || orgUnit.getPhones().size() == 0) {
			return;
		}

		for (PhoneType phoneType : PhoneType.values()) {
			List<Phone> phonesOfType = OrgUnitService.getPhones(orgUnit).stream().filter(p -> p.getPhoneType().equals(phoneType)).collect(Collectors.toList());
			
			if (phonesOfType.size() == 0) {
				continue;
			}

			int typePrimeCount = 0;
			for (Phone phone : phonesOfType) {
				if (phone.isTypePrime()) {
					typePrimeCount++;
				}
			}

			if (typePrimeCount != 1) {
				phonesOfType.forEach(p -> p.setTypePrime(false));
				phonesOfType.get(0).setTypePrime(true);
			}
		}
		
		int primes = 0;

		for (Phone phone : OrgUnitService.getPhones(orgUnit)) {
			if (phone.isPrime()) {
				primes++;
			}
		}
			
		if (primes != 1) {
			OrgUnitService.getPhones(orgUnit).forEach(p -> p.setPrime(false));

			for (Phone phone : OrgUnitService.getPhones(orgUnit)) {
				if (phone.isTypePrime()) {
					phone.setPrime(true);
					break;
				}
			}
		}
	}

	public void setPrimeEmail(OrgUnit orgUnit) {
		if (orgUnit.getEmails() != null && orgUnit.getEmails().size() > 0) {
			int primes = 0;

			for (Email email : OrgUnitService.getEmails(orgUnit)) {
				if (email.isPrime()) {
					primes++;
				}
			}
			
			if (primes != 1) {
				orgUnit.getEmails().get(0).getEmail().setPrime(true);
				
				for (int i = 1; i < orgUnit.getEmails().size(); i++) {
					orgUnit.getEmails().get(i).getEmail().setPrime(false);
				}
			}
		}
	}

	public void setPrimePost(OrgUnit orgUnit) {
		if (orgUnit.getPostAddresses() != null && orgUnit.getPostAddresses().size() > 0) {
			int primes = 0;

			for (Post post : OrgUnitService.getPosts(orgUnit)) {
				if (post.isPrime()) {
					primes++;
				}
			}
			
			if (primes != 1) {
				orgUnit.getPostAddresses().get(0).getPost().setPrime(true);
				
				for (int i = 1; i < orgUnit.getPostAddresses().size(); i++) {
					orgUnit.getPostAddresses().get(i).getPost().setPrime(false);
				}
			}
		}
	}
}
