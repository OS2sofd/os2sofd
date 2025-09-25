package dk.digitalidentity.sofd.interceptor;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.Person;

@Aspect
@Component
public class BeforeServiceSaveInterceptor {

	@Autowired
	private AbstractBeforeSaveInterceptor interceptor;
	
	@Transactional
	@Before("execution(* dk.digitalidentity.sofd.service.PersonService.save(dk.digitalidentity.sofd.dao.model.Person)) && args(person)")
	public void beforeSavePerson(Person person) {
		interceptor.handleSavePerson(person);
	}
	
	@Before("execution(* dk.digitalidentity.sofd.service.OrgUnitService.save(dk.digitalidentity.sofd.dao.model.OrgUnit)) && args(orgUnit)")
	public void beforeSaveOrgUnit(OrgUnit orgUnit) {
		interceptor.handleSaveOrgUnit(orgUnit);
	}

	@AfterReturning(pointcut = "execution(* dk.digitalidentity.sofd.service.PersonService.save(dk.digitalidentity.sofd.dao.model.Person)) && args(person)", returning = "retVal")
	public void handleAfterSavePerson(Person person, Person retVal) {
		interceptor.handleAccountOrders(retVal);
	}
}
