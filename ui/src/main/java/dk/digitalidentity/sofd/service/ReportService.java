package dk.digitalidentity.sofd.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.controller.mvc.dto.MultipleAffiliationsReportDTO;
import dk.digitalidentity.sofd.controller.mvc.dto.SofdAffiliationsReportDTO;
import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.User;

@Service
public class ReportService {
	@Autowired
	private SofdConfiguration configuration;
	@Autowired
	private PersonService personService;

	public List<Person> generateOpusButNoADReport() {
		List<Person> persons = personService.getActiveCached().stream()
				// Persons that have OPUS account
				.filter(p -> PersonService.getUsers(p).stream().anyMatch(u -> SupportedUserTypeService.isOpus(u.getUserType()) && u.isDisabled() == false))
				// Persons without AD account
				.filter(p -> PersonService.getUsers(p).stream().noneMatch(u -> SupportedUserTypeService.isActiveDirectory(u.getUserType()) && u.isDisabled() == false))
				.collect(Collectors.toList());

		return persons;
	}

	public List<Person> generateADWithoutAffiliationReport() {
		List<Person> persons = personService.getActiveCached().stream()
				// Persons that have AD account
				.filter(p -> PersonService.getUsers(p).stream().anyMatch(u -> SupportedUserTypeService.isActiveDirectory(u.getUserType()) && u.isDisabled() == false))
				// Persons without affiliations or persons that have all deleted or stopped affiliations
				.filter(p -> p.getAffiliations().isEmpty() ||
						p.getAffiliations().stream().allMatch(af -> (af.isDeleted()) || (AffiliationService.notActiveAnymore(af)))
				)
				.collect(Collectors.toList());

		return persons;
	}
	
	public List<Person> generateADWithoutActiveOpusAffiliationReport() {
		List<Person> persons = personService.getActiveCached().stream()
				// Persons that have AD account
				.filter(p -> PersonService.getUsers(p).stream().anyMatch(u -> SupportedUserTypeService.isActiveDirectory(u.getUserType()) && u.isDisabled() == false))
				// Persons without affiliations or persons that have all deleted or stopped affiliations (or non OPUS affiliations)
				.filter(p -> p.getAffiliations().stream().allMatch(af -> (!configuration.getModules().getLos().getPrimeAffiliationMaster().equals(af.getMaster()) || af.isDeleted()) || (AffiliationService.notActiveAnymore(af)))
				)
				.collect(Collectors.toList());

		return persons;
	}
	
	public List<Person> generateAccountOrdersDisabledReport() {
		List<Person> persons = personService.getActiveCached().stream()
				.filter(p -> p.isDisableAccountOrders())
				.collect(Collectors.toList());

		return persons;
	}
	
	public List<Person> generatePersonsForceStopReport() {
		List<Person> persons = personService.getActiveCached().stream()
				.filter(p -> p.isForceStop())
				.collect(Collectors.toList());

		return persons;
	}

	public List<Person> generatePersonsOnLeaveReport() {
		List<Person> persons = personService.getActiveCached().stream()
				.filter(p -> p.getLeave() != null)
				.collect(Collectors.toList());

		return persons;
	}

	public List<Person> generateDuplicateAffiliationReport() {
		List<Person> persons = new ArrayList<Person>();

		for (Person person : personService.getActiveCached()) {
			Set<String> orgUnits = new HashSet<>();

			// find all SOFD affiliations
			for (Affiliation  affiliation : person.getAffiliations()) {
				if (affiliation.getMaster().equals("SOFD")) {
					orgUnits.add(affiliation.getOrgUnit().getUuid());
				}
			}

			// find non-SOFD affiliations that maps to same OrgUnit as a SOFD-owned affiliation
			for (Affiliation  affiliation : person.getAffiliations()) {
				if (!affiliation.getMaster().equals("SOFD")) {
					if (orgUnits.contains(affiliation.getOrgUnit().getUuid())) {
						persons.add(person);
						break;
					}
				}
			}
		}

		return persons;
	}
	
	public List<MultipleAffiliationsReportDTO> generateMultipleAffiliationsReport() {
		List<MultipleAffiliationsReportDTO> multipleAffiliationsReportDTOs = new ArrayList<>();
		String wagesSystemMaster = configuration.getModules().getLos().getPrimeAffiliationMaster();

		for (Person person : personService.getActiveCached()) {

			// filter user accounts
			List<User> users = PersonService.getUsers(person).stream()
					.filter(u -> SupportedUserTypeService.isActiveDirectory(u.getUserType()) && u.isDisabled() == false)
					.collect(Collectors.toList());

			// filter affiliations
			List<Affiliation> affiliations = AffiliationService.notStoppedAffiliations(person.getAffiliations())
					.stream()
					.filter(a -> a.getMaster().equals(wagesSystemMaster))
					.collect(Collectors.toList());

			// at least one AD account and at least two "OPUS" affiliations
			if (users.size() >= 1 && affiliations.size() >= 2) {
				for (Affiliation affiliation : affiliations) {
					for (User user : users) {
						if (user.getEmployeeId() == null || user.getEmployeeId().equals(affiliation.getEmployeeId())) {
							MultipleAffiliationsReportDTO dto = new MultipleAffiliationsReportDTO();
							dto.setUuid(person.getUuid());
							dto.setName(PersonService.getName(person));
							dto.setCpr(PersonService.maskCpr(person.getCpr()));
							dto.setAffiliationName(AffiliationService.getPositionName(affiliation));
							dto.setAffilliationOrgUnitName(affiliation.getOrgUnit().getName());
							dto.setPrimeAffiliation(affiliation.isPrime());
							dto.setAffiliationTerms(affiliation.getEmploymentTermsText());
							dto.setEmployeeId(affiliation.getEmployeeId());
							dto.setUserId(user.getUserId());

							multipleAffiliationsReportDTOs.add(dto);
						}
					}
				}
			}
		}

		return multipleAffiliationsReportDTOs;
	}
	
	public List<SofdAffiliationsReportDTO> generateSofdAffiliationsReport() {
		List<SofdAffiliationsReportDTO> sofdAffiliationsReportDTO = new ArrayList<>();
		SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-DD");

		for (Person person : personService.getActiveCached()) {

			// filter affiliations
			List<Affiliation> affiliations = AffiliationService.notStoppedAffiliations(person.getAffiliations())
					.stream()
					.filter(a -> a.getMaster().equals("SOFD"))
					.collect(Collectors.toList());
			
			List<Affiliation> nonSofdAffiliations = AffiliationService.notStoppedAffiliations(person.getAffiliations())
					.stream()
					.filter(a -> !a.getMaster().equals("SOFD"))
					.collect(Collectors.toList());

			// at least one not-stopped SOFD affiliation, and no other non-stopped affiliations
			if (affiliations.size() >= 1 && nonSofdAffiliations.size() == 0) {
				for (Affiliation affiliation : affiliations) {
					SofdAffiliationsReportDTO dto = new SofdAffiliationsReportDTO();
					dto.setUuid(person.getUuid());
					dto.setName(PersonService.getName(person));
					dto.setCpr(PersonService.maskCpr(person.getCpr()));
					dto.setAffiliationName(AffiliationService.getPositionName(affiliation));
					dto.setAffilliationOrgUnitName(affiliation.getOrgUnit().getName());
					dto.setAffilliationVendor(affiliation.getVendor());
					dto.setAffilliationStartDate(affiliation.getStartDate() != null ? sdf.format(affiliation.getStartDate()) : "");
					dto.setAffilliationStopDate(affiliation.getStopDate() != null ? sdf.format(affiliation.getStopDate()) : "");

					sofdAffiliationsReportDTO.add(dto);
				}
			}
		}

		return sofdAffiliationsReportDTO;
	}
}
