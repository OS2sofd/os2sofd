package dk.digitalidentity.sofd.interceptor;

import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.Person;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
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

	// runs the interceptor and the save in one transaction, so modification history rows written by the
	// interceptor (including the fan-out to children and persons) are committed together with the orgunit
	@Around("execution(* dk.digitalidentity.sofd.service.OrgUnitService.save(dk.digitalidentity.sofd.dao.model.OrgUnit)) && args(orgUnit)")
	public Object aroundSaveOrgUnit(ProceedingJoinPoint joinPoint, OrgUnit orgUnit) throws Throwable {
		TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

		return transactionTemplate.execute(status -> {
			try {
				interceptor.handleSaveOrgUnit(orgUnit);

				return joinPoint.proceed();
			} catch (Throwable e) {
				status.setRollbackOnly();
				throw new RuntimeException(e);
			}
		});
	}
}