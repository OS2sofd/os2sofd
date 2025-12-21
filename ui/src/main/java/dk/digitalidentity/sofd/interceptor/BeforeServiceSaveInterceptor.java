package dk.digitalidentity.sofd.interceptor;

import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.Person;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Aspect
@Component
public class BeforeServiceSaveInterceptor {

	@Autowired
	private AbstractBeforeSaveInterceptor interceptor;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Around("execution(* dk.digitalidentity.sofd.service.PersonService.save(dk.digitalidentity.sofd.dao.model.Person)) && args(person)")
	public Object aroundSavePerson(ProceedingJoinPoint joinPoint, Person person) throws Throwable {
		TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

		return transactionTemplate.execute(status -> {
			try {
				// before save
				interceptor.handleSavePerson(person);

				// actual save
				Person result = (Person) joinPoint.proceed();

				// after save
				interceptor.handleAccountOrders(result);

				return result;
			} catch (Throwable e) {
				status.setRollbackOnly();
				throw new RuntimeException(e);
			}
		});
	}

	@Before("execution(* dk.digitalidentity.sofd.service.OrgUnitService.save(dk.digitalidentity.sofd.dao.model.OrgUnit)) && args(orgUnit)")
	public void beforeSaveOrgUnit(OrgUnit orgUnit) {
		interceptor.handleSaveOrgUnit(orgUnit);
	}
}