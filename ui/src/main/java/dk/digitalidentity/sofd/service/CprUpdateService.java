package dk.digitalidentity.sofd.service;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import dk.digitalidentity.sofd.security.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.controller.mvc.dto.BadStateDTO;
import dk.digitalidentity.sofd.controller.mvc.dto.CprLookupChildDTO;
import dk.digitalidentity.sofd.controller.mvc.dto.CprLookupDTO;
import dk.digitalidentity.sofd.dao.model.Child;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.Post;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CprUpdateService {

	@Autowired
	private CprService cprService;

	@Autowired
	private PersonService personService;

	@Autowired
	private SofdConfiguration configuration;

	@Transactional
	public void updateBadState() {
		List<BadStateDTO> badStates = cprService.getBadStates();
		Map<String, BadStateDTO> badStateMap = badStates.stream().collect(Collectors.toMap(BadStateDTO::getCpr, Function.identity()));

		List<Person> activePersons = personService.getActive();
		
		for (Person person : activePersons) {
			BadStateDTO badState = badStateMap.get(person.getCpr());
			if (badState != null) {
				if ((!person.isDead() && badState.isDead()) || (!person.isDisenfranchised() && badState.isDisenfranchised())) {
					
					log.info("Bad state detected on " + PersonService.getName(person) + " / " + person.getUuid());
					
					CprLookupDTO cprLookupDTO = cprService.getByCpr(person.getCpr(), true);
					
					// TODO: semi-bad duplicate code from below
					if (cprLookupDTO != null) {
						Post cprPost = null;
						String firstname = cprLookupDTO.getFirstname();
						String surname = cprLookupDTO.getLastname();

						if (cprLookupDTO.hasAddress()) {
							cprPost = Post.builder()
									.master("SOFD")
									.masterId(UUID.randomUUID().toString())
									.prime(true)
									.addressProtected(cprLookupDTO.isAddressProtected())
									.city(cprLookupDTO.getCity())
									.country(cprLookupDTO.getCountry())
									.localname(cprLookupDTO.getLocalname())
									.postalCode(cprLookupDTO.getPostalCode())
									.street(cprLookupDTO.getStreet())
									.build();
						}

						personService.updatePersonFromCpr(person, cprPost, firstname, surname, cprLookupDTO.isDead(), cprLookupDTO.isDisenfranchised());
					}
				}
			}
		}
	}
	
	public void updatePersonsWithLastCprDigit(String digit) {
		Authentication authentication = SecurityUtil.getLoginSession();
		try {
			SecurityUtil.fakeLoginSession();

			// preload data, so we do not need a transaction
			List<Person> activePersons = personService.getActive(p -> {
				if (p.getChildren() != null && p.getChildren().size() > 0) {
					p.getChildren().forEach(c -> {
						c.getName();
					});
				}
			});

			int count = 0;
			for (Person person : activePersons) {
				if (person.getCpr().endsWith(digit) || !person.isUpdatedFromCpr()) {
					syncPerson(person);
					count++;
				}
			}

			log.info("Verified " + count + " persons against cpr");
		}
		finally {
			SecurityUtil.setLoginSession(authentication);
		}
	}

	@Transactional
	public void updatePerson(String personUuid) {
		var person = personService.getByUuid(personUuid);
		if (person != null) {
			syncPerson(person);
		}
	}

	private void syncPerson(Person person) {
		CprLookupDTO cprLookupDTO = cprService.getByCpr(person.getCpr());

		if (cprLookupDTO != null) {
			Post cprPost = null;
			String firstname = cprLookupDTO.getFirstname();
			String surname = cprLookupDTO.getLastname();

			if (cprLookupDTO.hasAddress()) {
				cprPost = Post.builder()
						.master("SOFD")
						.masterId(UUID.randomUUID().toString())
						.prime(true)
						.addressProtected(cprLookupDTO.isAddressProtected())
						.city(cprLookupDTO.getCity())
						.country(cprLookupDTO.getCountry())
						.localname(cprLookupDTO.getLocalname())
						.postalCode(cprLookupDTO.getPostalCode())
						.street(cprLookupDTO.getStreet())
						.build();
			}

			if ((cprPost != null && !comparePost(cprPost, person.getRegisteredPostAddress())) ||
					!firstname.equals(person.getFirstname()) ||
					!surname.equals(person.getSurname()) ||
					!Objects.equals(person.isDead(), cprLookupDTO.isDead()) ||
					!Objects.equals(person.isDisenfranchised(), cprLookupDTO.isDisenfranchised())) {

				personService.updatePersonFromCpr(person, cprPost, firstname, surname, cprLookupDTO.isDead(), cprLookupDTO.isDisenfranchised());
			}

			if (configuration.getScheduled().getCprSync().isEnabledChildren()) {
				List<CprLookupChildDTO> children = cprLookupDTO.getChildren();

				if (children != null) {

					// filter out children that are too old
					children.removeIf(
							c -> getAgeFromCpr(c.getCpr()) > configuration.getIntegrations().getChildren().getMaxAge());

					boolean shouldSave = false;

					// handle inserts and updates
					for (CprLookupChildDTO child : children) {
						Child existingChild = person.getChildren().stream()
								.filter(c -> Objects.equals(c.getCpr(), child.getCpr())).findFirst().orElse(null);
						if (existingChild == null) {
							CprLookupDTO childData = cprService.getByCpr(child.getCpr());

							Child newChild = new Child();
							newChild.setCpr(child.getCpr());
							newChild.setName(
									(childData != null) ? (childData.getFirstname() + " " + childData.getLastname())
											: "Ukendt");
							newChild.setParent(person);
							person.getChildren().add(newChild);

							log.info("Adding child (" + newChild.getName() + ") to " + person.getUuid());

							shouldSave = true;
						} else if ("Ukendt".equals(existingChild.getName())) {
							CprLookupDTO childData = cprService.getByCpr(child.getCpr(), true);

							if (childData != null) {
								existingChild.setName(childData.getFirstname() + " " + childData.getLastname());

								log.info("Adding child (" + existingChild.getName() + ") on " + person.getUuid());

								shouldSave = true;
							}
						}
					}

					// handle deletes
					List<Child> toBeDeleted = person.getChildren().stream()
							.filter(sofdChild -> children.stream().noneMatch(
									serviceChild -> Objects.equals(serviceChild.getCpr(), sofdChild.getCpr())))
							.collect(Collectors.toList());

					if (toBeDeleted.size() > 0) {
						log.info("Removing " + toBeDeleted.size() + " child(ren) from " + person.getUuid());

						person.getChildren().removeAll(toBeDeleted);
						shouldSave = true;
					}

					if (shouldSave) {
						personService.save(person);
					}
				}
			}
		}
	}

	private int getAgeFromCpr(String cpr) {
		try {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("ddMMyy");
			LocalDate birthDate = LocalDate.parse(cpr.substring(0, 6), formatter);

			return Period.between(birthDate, LocalDate.now()).getYears();
		} catch (Exception ex) {
			log.error("Failed to parse cpr: " + cpr);

			// make sure the child gets filtered out
			return 200;
		}
	}

	private boolean comparePost(Post post1, Post post2) {
		if (post1 == null && post2 == null) {
			return true;
		}

		if (post1 == null && post2 != null) {
			return false;
		}

		if (post1 != null && post2 == null) {
			return false;
		}

		// from here, both are non-null

		if (!Objects.equals(post1.isAddressProtected(), post2.isAddressProtected())) {
			return false;
		}

		if (!Objects.equals(post1.getCity(), post2.getCity())) {
			return false;
		}

		if (!Objects.equals(post1.getCountry(), post2.getCountry())) {
			return false;
		}

		if (!Objects.equals(post1.getLocalname(), post2.getLocalname())) {
			return false;
		}

		if (!Objects.equals(post1.getPostalCode(), post2.getPostalCode())) {
			return false;
		}

		if (!Objects.equals(post1.getStreet(), post2.getStreet())) {
			return false;
		}

		// found no differences that matter
		return true;
	}

	@Transactional
	public void updateAllPersons() {
		Authentication authentication = SecurityUtil.getLoginSession();
		try {
			SecurityUtil.fakeLoginSession();

			List<Person> activePersons = personService.getActive();

			int count = 0;
			for (Person person : activePersons) {
				syncPerson(person);
				count++;
				if( count % 100 == 0) {
					log.info("Updated " + count + " persons out of " + activePersons.size());
				}
			}

			log.info("Verified " + count + " persons against cpr");
		}
		finally {
			SecurityUtil.setLoginSession(authentication);
		}
	}
}
