package dk.digitalidentity.sofd.task;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.OrgUnitManager;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.enums.EmailTemplateType;
import dk.digitalidentity.sofd.security.SecurityUtil;
import dk.digitalidentity.sofd.service.AffiliationService;
import dk.digitalidentity.sofd.service.ManagerService;
import dk.digitalidentity.sofd.service.OrgUnitService;
import dk.digitalidentity.sofd.service.PersonService;
import lombok.extern.slf4j.Slf4j;

@Component
@EnableScheduling
@Slf4j
public class SynchronizeOrgUnitManagersTask {

	@Autowired
	private SofdConfiguration configuration;

	@Autowired
	private OrgUnitService orgUnitService;

	@Autowired
	private PersonService personService;
	
	@Autowired
	private ManagerService managerService;

	// should run AFTER the load from OPUS
	@Scheduled(cron = "${cron.managersync:0 30 5 * * ?}")
	@Transactional(rollbackFor = Exception.class)
	public void processChanges() throws Exception {
		// TODO: if the API call hits the instance where synchronization is disabled, it bypasses this... we should have a boolean
		//       argument that allows bypass - and a wrapper method, as scheduled tasks cannot take arguments ;)
		if (!configuration.getScheduled().isEnabled()) {
			log.debug("Scheduled jobs are disabled on this instance");
			return;
		}
		if (!configuration.getScheduled().getManagerSync().isEnabled()) {
			log.debug("ManagerSync is disabled on this instance");
			return;
		}
		if (configuration.getModules().getManager().isEditEnabled()) {
			log.debug("Manager edit is enabled so not running ManagerSync");
			return;
		}

		SecurityUtil.fakeLoginSession();

		// Remove all current manager links
		List<OrgUnit> allOrgUnits = orgUnitService.getAll();
		List<Person> allManagers = personService.findAllManagers();

		if (allManagers != null) {
			for (Person person : allManagers) {

				if (person.getAffiliations() != null) {
					for (Affiliation aff : person.getAffiliations()) {

						if (aff.getManagerFor() != null) {
							for (OrgUnit orgUnit : AffiliationService.getManagerFor(aff)) {
								OrgUnitManager manager = new OrgUnitManager();
								manager.setInherited(false);
								manager.setManager(person);
								manager.setOrgUnit(orgUnit);
								manager.setName(person.getChosenName() != null ? person.getChosenName() : person.getFirstname() + " " + person.getSurname());
								orgUnit.setNewManager(manager);

								setManager(orgUnit, person);
							}
						}
					}
				}
			}
		}

		int counter = 0;

		for (OrgUnit orgUnit : allOrgUnits) {
			boolean changes = false;

			// readability over efficiency :)
			if (orgUnit.getManager() == null && orgUnit.getNewManager() == null) {
				changes = false;
			}
			else if (orgUnit.getManager() != null && orgUnit.getNewManager() == null) {
				changes = true;
			}
			else if (orgUnit.getManager() == null && orgUnit.getNewManager() != null) {
				changes = true;
			}
			else if (!orgUnit.getManager().getManager().getUuid().equals(orgUnit.getNewManager().getManager().getUuid())) {
				changes = true;
			}

			if (changes) {
				// check for new manager mail
				if (orgUnit.getNewManager() != null && !orgUnit.getNewManager().isInherited()) {
					boolean sendMail = true;
					if (orgUnit.getManager() != null) {
						if (!orgUnit.getNewManager().getManager().getUuid().equals(orgUnit.getManager().getManager().getUuid()) || orgUnit.getManager().isInherited()) {
							sendMail = true;
						} else {
							sendMail = false;
						}
					}
					
					if (sendMail) {
						managerService.sendMail(orgUnit, EmailTemplateType.NEW_MANAGER, orgUnit.getNewManager().getName());
					}
				}
				
				// check for manager removed
				if (orgUnit.getManager() != null && !orgUnit.getManager().isInherited() && (orgUnit.getNewManager() == null || (orgUnit.getNewManager() != null && orgUnit.getNewManager().isInherited()))) {
					managerService.sendMail(orgUnit, EmailTemplateType.MANAGER_REMOVED, orgUnit.getManager().getName());
				}
				
				orgUnit.setManager(orgUnit.getNewManager());

				orgUnitService.save(orgUnit);
				counter++;
			}
		}

		log.info("Updated " + counter + " managers");
	}

	private void setManager(OrgUnit orgUnit, Person person) {
		for (OrgUnit child : orgUnit.getChildren()) {

			if (child.getNewManager() != null && !child.getNewManager().isInherited()) {
				// already has a real manager. we skip.
			}
			else {
				// set inherited manager
				OrgUnitManager manager = new OrgUnitManager();
				manager.setInherited(true);
				manager.setManager(person);
				manager.setOrgUnit(child);
				manager.setName(person.getChosenName() != null ? person.getChosenName() : person.getFirstname() + " " + person.getSurname());
				child.setNewManager(manager);

				setManager(child, person);
			}
		}
	}
}