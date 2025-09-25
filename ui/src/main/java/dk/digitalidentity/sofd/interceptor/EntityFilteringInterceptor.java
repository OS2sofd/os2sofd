package dk.digitalidentity.sofd.interceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Component;

import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.Client;
import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.enums.AccessEntity;
import dk.digitalidentity.sofd.dao.model.enums.AccessEntityField;
import dk.digitalidentity.sofd.exception.InsufficientAccessRightException;
import dk.digitalidentity.sofd.security.SecurityUtil;
import lombok.extern.slf4j.Slf4j;

// TODO: this entire thing goes away once we kill SDR (remember to require WRITE_ACCESS to access v2 API)
@Slf4j
@Aspect
@Component
public class EntityFilteringInterceptor {

	@AfterReturning(pointcut = "execution(* dk.digitalidentity.sofd.dao.PersonDao.get*(..))", returning = "target")
	public void afterGetPerson(Object target) throws Throwable {
		processTarget(target);
	}

	@AfterReturning(pointcut = "execution(* dk.digitalidentity.sofd.dao.PersonDao.find*(..))", returning = "target")
	public void afterFindPerson(Object target) throws Throwable {
		processTarget(target);
	}

	@AfterReturning(pointcut = "execution(* dk.digitalidentity.sofd.dao.OrgUnitDao.get*(..))", returning = "target")
	public void afterGetOrgUnit(Object target) throws Throwable {
		processTarget(target);
	}

	@AfterReturning(pointcut = "execution(* dk.digitalidentity.sofd.dao.OrgUnitDao.find*(..))", returning = "target")
	public void afterFindOrgUnit(Object target) throws Throwable {
		processTarget(target);
	}

	@SuppressWarnings("unchecked")
	private void processTarget(Object target) throws Exception {
		if (target == null) {
			return;
		}

		if (target instanceof Person) {
			Person person = (Person) target;
			filterPersonWhenRead(person);
		}
		else if (target instanceof OrgUnit) {
			OrgUnit orgUnit = (OrgUnit) target;
			filterOrgUnitWhenRead(orgUnit);
		}
		else if (target instanceof PageImpl<?>) {
			Page<Object> page = (Page<Object>) target;
			List<Object> targetList = page.getContent();

			if (!targetList.isEmpty()) {
				// Get first element of list to determine type
				if (targetList.get(0) instanceof Person) {
					List<Person> persons = targetList.stream().map(o -> (Person) o).collect(Collectors.toList());

					for (Person person : persons) {
						filterPersonWhenRead(person);
					}
				}
				else if (targetList.get(0) instanceof OrgUnit) {
					List<OrgUnit> ous = targetList.stream().map(o -> (OrgUnit) o).collect(Collectors.toList());

					for (OrgUnit ou : ous) {
						filterOrgUnitWhenRead(ou);
					}
				}
			}
		}
	}

	private void filterPersonWhenRead(Person person) {
		Client client = SecurityUtil.getClient();

		if (client != null) {
			switch (client.getAccessRole()) {
			case WRITE_ACCESS:
			case READ_ACCESS:
				// Do nothing Client has full access
				break;
			case LIMITED_READ_ACCESS:
				restrictAccessToPersonFields(person, client);
				break;
			}
		}
	}

	private void restrictAccessToPersonFields(Person person, Client client) {
		List<AccessEntityField> userFieldList = client.getAccessFieldList().stream().filter(af -> af.getEntity().equals(AccessEntity.PERSON)).map(af -> af.getAccessEntityField()).collect(Collectors.toList());
		List<AccessEntityField> toBeCleaned = new ArrayList<AccessEntityField>(AccessEntityField.getAllPersonFields());
		toBeCleaned.removeAll(userFieldList);

		if (userFieldList.isEmpty()) {
			// if Client doesn't have access rights to any of the properties we throw an error
			throw new InsufficientAccessRightException();
		}

		for (AccessEntityField accessEntityField : toBeCleaned) {
			switch (accessEntityField) {
			case PERSON_BASIC:
				// this one exist just for completeness sake, but we never actually filter these values
				break;
			case PERSON_ADDRESS:
				person.setRegisteredPostAddress(null);
				person.setResidencePostAddress(null);
				break;
			case PERSON_PHONE:
				person.setPhones(null);
				break;
			case PERSON_AFFILIATIONS:
				person.setAffiliations(null);
				break;
			case PERSON_AFFILIATIONS_DETAILS:
				if (person.getAffiliations() != null) {
					for (Affiliation affiliation : person.getAffiliations()) {
						affiliation.setEmploymentTerms(null);
						affiliation.setEmploymentTermsText(null);
						affiliation.setPayGrade(null);
						affiliation.setWorkingHoursDenominator(null);
						affiliation.setWorkingHoursNumerator(null);
					}
				}
				break;
			case PERSON_CPR:
				person.setCpr(null);
				break;
			case PERSON_USER:
				person.setUsers(null);
				break;
			default:
				if (accessEntityField.name().startsWith("PERSON_")) {
					log.error("AccessEntityField: " + accessEntityField.name().toString() + " unhandled!");
					throw new InsufficientAccessRightException();
				}
				break;
			}
		}
	}

	private void filterOrgUnitWhenRead(OrgUnit orgUnit) {
		Client client = SecurityUtil.getClient();

		if (client != null) {
			switch (client.getAccessRole()) {
			case WRITE_ACCESS:
			case READ_ACCESS:
				// Do nothing Client has full access
				break;
			case LIMITED_READ_ACCESS:
				restrictAccessToOrgUnitFields(orgUnit, client);
				break;
			}
		}
	}

	private void restrictAccessToOrgUnitFields(OrgUnit orgUnit, Client client) {
		List<AccessEntityField> orgUnitFieldList = client.getAccessFieldList().stream().filter(af -> af.getEntity().equals(AccessEntity.ORGUNIT)).map(af -> af.getAccessEntityField()).collect(Collectors.toList());
		List<AccessEntityField> toBeCleaned = new ArrayList<AccessEntityField>(AccessEntityField.getAllOrgunitFields());
		toBeCleaned.removeAll(orgUnitFieldList);

		if (orgUnitFieldList.isEmpty()) {
			// if Client doesn't have access rights to any of the properties we throw an error
			throw new InsufficientAccessRightException();
		}

		for (AccessEntityField accessEntityField : toBeCleaned) {
			switch (accessEntityField) {
			case ORGUNIT_BASIC:
				// this one exist just for completeness sake, but we never actually filter these values
				break;
			case ORGUNIT_ADDRESS:
				orgUnit.setPostAddresses(null);
				orgUnit.setEmail(null);
				break;
			case ORGUNIT_PHONE:
				orgUnit.setPhones(null);
				break;
			case ORGUNIT_AFFILIATIONS:
				orgUnit.setAffiliations(null);
				break;
			case ORGUNIT_AFFILIATIONS_DETAILS:
				if (orgUnit.getAffiliations() != null) {
					for (Affiliation affiliation : orgUnit.getAffiliations()) {
						affiliation.setEmploymentTerms(null);
						affiliation.setEmploymentTermsText(null);
						affiliation.setPayGrade(null);
						affiliation.setWorkingHoursDenominator(null);
						affiliation.setWorkingHoursNumerator(null);
					}
				}
				break;
			case ORGUNIT_KLE:
				orgUnit.setKlePrimary(null);
				orgUnit.setKleSecondary(null);
				break;
			case ORGUNIT_MANAGER:
				orgUnit.setManager(null);
				break;
			default:
				if (accessEntityField.name().startsWith("ORGUNIT_")) {
					log.error("AccessEntityField: " + accessEntityField.name().toString() + " unhandled!");
					throw new InsufficientAccessRightException();
				}
				break;
			}
		}
	}
}
