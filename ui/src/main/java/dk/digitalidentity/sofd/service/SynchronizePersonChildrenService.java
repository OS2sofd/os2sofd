package dk.digitalidentity.sofd.service;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.controller.mvc.dto.CprLookupChildDTO;
import dk.digitalidentity.sofd.controller.mvc.dto.CprLookupDTO;
import dk.digitalidentity.sofd.dao.model.Child;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.security.SecurityUtil;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SynchronizePersonChildrenService {
    
    @Autowired
    private CprService cprService;

    @Autowired
    private PersonService personService;

	@Autowired
	private SofdConfiguration configuration;

	@Transactional
	public void updateChildrenOnAllWithLastCprDigit(String digit) {
        List<Person> activePersons = personService.getAll();

        for (Person person : activePersons) {
        	if (person.getCpr().endsWith(digit)) {
        		updatePersonChildren(person.getUuid());
        	}
        }
	}
	
	private int getAgeFromCpr(String cpr) {
		try {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("ddMMyy");
			LocalDate birthDate = LocalDate.parse(cpr.substring(0, 6), formatter);
			int age = Period.between(birthDate, LocalDate.now()).getYears();
			return age;
		}
		catch (Exception ex) {
			log.error("Failed to parse cpr: " + cpr);

			// make sure the child gets filtered out
			return 200;
		}
	}

	private void updatePersonChildren(String personUuid) {
    	Person person = personService.getByUuid(personUuid);
		Date yesterday = java.sql.Date.valueOf(LocalDate.now().plusDays(-1));
		Date tomorrow = java.sql.Date.valueOf(LocalDate.now().plusDays(1));

    	boolean hasValidAffiliation = !person.isDeleted() && person.getAffiliations().stream().anyMatch(a ->
				(a.getStartDate() == null || a.getStartDate().before(tomorrow))
				&& (a.getStopDate() == null || a.getStopDate().after(yesterday))
				&& configuration.getIntegrations().getChildren().getAffiliationMasters().contains(a.getMaster())
		);

    	List<CprLookupChildDTO> childrenTmp = new ArrayList<>();
    	if (hasValidAffiliation) {
    		CprLookupDTO dto = cprService.getByCpr(person.getUuid(), true);
    		if (dto != null) {
    			childrenTmp = dto.getChildren();
    		}
    		else {
    			// to avoid null'ing all children
    			childrenTmp = null;
    		}
    	}

        if (childrenTmp != null) {
        	final List<CprLookupChildDTO> children = childrenTmp;

        	// filter out children that are too old
        	children.removeIf(c -> getAgeFromCpr(c.getCpr()) > configuration.getIntegrations().getChildren().getMaxAge());
        	
			boolean shouldSave = false;

			// Handle inserts and updates
			for (CprLookupChildDTO child : children) {
				String name = "Ukendt";

				Optional<Child> existingChild = person.getChildren().stream().filter(c -> c.getCpr().equalsIgnoreCase(child.getCpr())).findFirst();
				if (!existingChild.isPresent()) {
					Child newChild = new Child();
					newChild.setCpr(child.getCpr());
					newChild.setName(name);
					newChild.setParent(person);
					person.getChildren().add(newChild);

					shouldSave = true;
				}
				else {
					Child sofdChild = existingChild.get();
					
					if (!sofdChild.getName().equalsIgnoreCase(name)) {
						sofdChild.setName(name);
						shouldSave = true;
					}
				}
			}

			// Handle deletes
			List<Child> toBeDeleted = person.getChildren().stream().filter(sofdChild -> children.stream().noneMatch(serviceChild -> serviceChild.getCpr().equalsIgnoreCase(sofdChild.getCpr()))).collect(Collectors.toList());
			if (toBeDeleted.size() > 0) {
				person.getChildren().removeAll(toBeDeleted);
				shouldSave = true;
			}

			if (shouldSave) {
				SecurityUtil.fakeLoginSession();
				personService.save(person);
			}
        }
    }
}
