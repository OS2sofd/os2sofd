package dk.digitalidentity.sofd.log;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import dk.digitalidentity.sofd.dao.model.Client;
import dk.digitalidentity.sofd.dao.model.enums.EventType;

@Aspect
@Component
public class AuditInterceptor {

	@Autowired
	private AuditLogger auditLogger;

	@AfterReturning(pointcut = "execution(* dk.digitalidentity.sofd.service.ClientService.save(dk.digitalidentity.sofd.dao.model.Client)) && args(client)", returning = "retVal")
	public void interceptSaveClient(Client client, Client retVal) {
		auditLogger.log(retVal, EventType.SAVE, retVal.getEntityLogInfo());
	}

	@Before("execution(* dk.digitalidentity.sofd.service.ClientService.delete(dk.digitalidentity.sofd.dao.model.Client)) && args(client)")
	public void interceptDeleteClient(Client client) {
		auditLogger.log(client, EventType.DELETE, client.getEntityLogInfo());
	}

	/* TODO: skal på værksted, giver crazy farlige fejl i produktion

	2021-02-08 11:27:38.782 ERROR 7 --- [o-8080-exec-478] o.a.c.c.C.[.[.[/].[dispatcherServlet]    : Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception [Request processing failed; nested exception is org.springframework.transaction.TransactionSystemException: Could not commit JPA transaction; nested exception is javax.persistence.RollbackException: Error while committing the transaction] with root cause

	javax.validation.ConstraintViolationException: Validation failed for classes [dk.digitalidentity.sofd.dao.model.User] during persist time for groups [javax.validation.groups.Default, ]
	List of constraint violations:[
		ConstraintViolationImpl{interpolatedMessage='may not be null', propertyPath=uuid, rootBeanClass=class dk.digitalidentity.sofd.dao.model.User, messageTemplate='{javax.validation.constraints.NotNull.message}'}
		ConstraintViolationImpl{interpolatedMessage='may not be null', propertyPath=master, rootBeanClass=class dk.digitalidentity.sofd.dao.model.User, messageTemplate='{javax.validation.constraints.NotNull.message}'}
		ConstraintViolationImpl{interpolatedMessage='may not be null', propertyPath=masterId, rootBeanClass=class dk.digitalidentity.sofd.dao.model.User, messageTemplate='{javax.validation.constraints.NotNull.message}'}
	]
		at org.hibernate.cfg.beanvalidation.BeanValidationEventListener.validate(BeanValidationEventListener.java:138) ~[hibernate-core-5.0.12.Final.jar!/:5.0.12.Final]
		at org.hibernate.cfg.beanvalidation.BeanValidationEventListener.onPreInsert(BeanValidationEventListener.java:78) ~[hibernate-core-5.0.12.Final.jar!/:5.0.12.Final]
		at org.hibernate.action.internal.EntityIdentityInsertAction.preInsert(EntityIdentityInsertAction.java:197) ~[hibernate-core-5.0.12.Final.jar!/:5.0.12.Final]
		at org.hibernate.action.internal.EntityIdentityInsertAction.execute(EntityIdentityInsertAction.java:75) ~[hibernate-core-5.0.12.Final.jar!/:5.0.12.Final]
		at org.hibernate.engine.spi.ActionQueue.execute(ActionQueue.java:619) ~[hibernate-core-5.0.12.Final.jar!/:5.0.12.Final]
		at org.hibernate.engine.spi.ActionQueue.addResolvedEntityInsertAction(ActionQueue.java:273) ~[hibernate-core-5.0.12.Final.jar!/:5.0.12.Final]
		at org.hibernate.engine.spi.ActionQueue.addInsertAction(ActionQueue.java:254) ~[hibernate-core-5.0.12.Final.jar!/:5.0.12.Final]
		at org.hibernate.engine.spi.ActionQueue.addAction(ActionQueue.java:299) ~[hibernate-core-5.0.12.Final.jar!/:5.0.12.Final]
		at org.hibernate.event.internal.AbstractSaveEventListener.addInsertAction(AbstractSaveEventListener.java:317) ~[hibernate-core-5.0.12.Final.jar!/:5.0.12.Final]
		at org.hibernate.event.internal.AbstractSaveEventListener.performSaveOrReplicate(AbstractSaveEventListener.java:272) ~[hibernate-core-5.0.12.Final.jar!/:5.0.12.Final]
		at org.hibernate.event.internal.AbstractSaveEventListener.performSave(AbstractSaveEventListener.java:178) ~[hibernate-core-5.0.12.Final.jar!/:5.0.12.Final]
		at org.hibernate.event.internal.AbstractSaveEventListener.saveWithGeneratedId(AbstractSaveEventListener.java:109) ~[hibernate-core-5.0.12.Final.jar!/:5.0.12.Final]
		at org.hibernate.jpa.event.internal.core.JpaPersistEventListener.saveWithGeneratedId(JpaPersistEventListener.java:67) ~[hibernate-entitymanager-5.0.12.Final.jar!/:5.0.12.Final]
		at org.hibernate.event.internal.DefaultPersistEventListener.entityIsTransient(DefaultPersistEventListener.java:189) ~[hibernate-core-5.0.12.Final.jar!/:5.0.12.Final]
		at org.hibernate.event.internal.DefaultPersistEventListener.onPersist(DefaultPersistEventListener.java:132) ~[hibernate-core-5.0.12.Final.jar!/:5.0.12.Final]
		at org.hibernate.internal.SessionImpl.firePersistOnFlush(SessionImpl.java:802) ~[hibernate-core-5.0.12.Final.jar!/:5.0.12.Final]
		at org.hibernate.internal.SessionImpl.persistOnFlush(SessionImpl.java:795) ~[hibernate-core-5.0.12.Final.jar!/:5.0.12.Final]
		at org.hibernate.engine.spi.CascadingActions$8.cascade(CascadingActions.java:340) ~[hibernate-core-5.0.12.Final.jar!/:5.0.12.Final]
		at org.hibernate.engine.internal.Cascade.cascadeToOne(Cascade.java:398) ~[hibernate-core-5.0.12.Final.jar!/:5.0.12.Final]
		at org.hibernate.engine.internal.Cascade.cascadeAssociation(Cascade.java:323) ~[hibernate-core-5.0.12.Final.jar!/:5.0.12.Final]
		at org.hibernate.engine.internal.Cascade.cascadeProperty(Cascade.java:162) ~[hibernate-core-5.0.12.Final.jar!/:5.0.12.Final]
		at org.hibernate.engine.internal.Cascade.cascadeCollectionElements(Cascade.java:431) ~[hibernate-core-5.0.12.Final.jar!/:5.0.12.Final]
		at org.hibernate.engine.internal.Cascade.cascadeCollection(Cascade.java:363) ~[hibernate-core-5.0.12.Final.jar!/:5.0.12.Final]
		at org.hibernate.engine.internal.Cascade.cascadeAssociation(Cascade.java:326) ~[hibernate-core-5.0.12.Final.jar!/:5.0.12.Final]
		at org.hibernate.engine.internal.Cascade.cascadeProperty(Cascade.java:162) ~[hibernate-core-5.0.12.Final.jar!/:5.0.12.Final]
		at org.hibernate.engine.internal.Cascade.cascade(Cascade.java:111) ~[hibernate-core-5.0.12.Final.jar!/:5.0.12.Final]
		at org.hibernate.event.internal.AbstractFlushingEventListener.cascadeOnFlush(AbstractFlushingEventListener.java:150) ~[hibernate-core-5.0.12.Final.jar!/:5.0.12.Final]
		at org.hibernate.event.internal.AbstractFlushingEventListener.prepareEntityFlushes(AbstractFlushingEventListener.java:141) ~[hibernate-core-5.0.12.Final.jar!/:5.0.12.Final]
		at org.hibernate.event.internal.AbstractFlushingEventListener.flushEverythingToExecutions(AbstractFlushingEventListener.java:74) ~[hibernate-core-5.0.12.Final.jar!/:5.0.12.Final]
		at org.hibernate.event.internal.DefaultFlushEventListener.onFlush(DefaultFlushEventListener.java:38) ~[hibernate-core-5.0.12.Final.jar!/:5.0.12.Final]
		at org.hibernate.internal.SessionImpl.flush(SessionImpl.java:1282) ~[hibernate-core-5.0.12.Final.jar!/:5.0.12.Final]
		at org.hibernate.internal.SessionImpl.managedFlush(SessionImpl.java:465) ~[hibernate-core-5.0.12.Final.jar!/:5.0.12.Final]
		at org.hibernate.internal.SessionImpl.flushBeforeTransactionCompletion(SessionImpl.java:2963) ~[hibernate-core-5.0.12.Final.jar!/:5.0.12.Final]
		at org.hibernate.internal.SessionImpl.beforeTransactionCompletion(SessionImpl.java:2339) ~[hibernate-core-5.0.12.Final.jar!/:5.0.12.Final]
		at org.hibernate.engine.jdbc.internal.JdbcCoordinatorImpl.beforeTransactionCompletion(JdbcCoordinatorImpl.java:485) ~[hibernate-core-5.0.12.Final.jar!/:5.0.12.Final]
		at org.hibernate.resource.transaction.backend.jdbc.internal.JdbcResourceLocalTransactionCoordinatorImpl.beforeCompletionCallback(JdbcResourceLocalTransactionCoordinatorImpl.java:147) ~[hibernate-core-5.0.12.Final.jar!/:5.0.12.Final]
		at org.hibernate.resource.transaction.backend.jdbc.internal.JdbcResourceLocalTransactionCoordinatorImpl.access$100(JdbcResourceLocalTransactionCoordinatorImpl.java:38) ~[hibernate-core-5.0.12.Final.jar!/:5.0.12.Final]
		at org.hibernate.resource.transaction.backend.jdbc.internal.JdbcResourceLocalTransactionCoordinatorImpl$TransactionDriverControlImpl.commit(JdbcResourceLocalTransactionCoordinatorImpl.java:231) ~[hibernate-core-5.0.12.Final.jar!/:5.0.12.Final]
		at org.hibernate.engine.transaction.internal.TransactionImpl.commit(TransactionImpl.java:65) ~[hibernate-core-5.0.12.Final.jar!/:5.0.12.Final]
		at org.hibernate.jpa.internal.TransactionImpl.commit(TransactionImpl.java:61) ~[hibernate-entitymanager-5.0.12.Final.jar!/:5.0.12.Final]
		at org.springframework.orm.jpa.JpaTransactionManager.doCommit(JpaTransactionManager.java:517) ~[spring-orm-4.3.25.RELEASE.jar!/:4.3.25.RELEASE]
		at org.springframework.transaction.support.AbstractPlatformTransactionManager.processCommit(AbstractPlatformTransactionManager.java:765) ~[spring-tx-4.3.25.RELEASE.jar!/:4.3.25.RELEASE]
		at org.springframework.transaction.support.AbstractPlatformTransactionManager.commit(AbstractPlatformTransactionManager.java:734) ~[spring-tx-4.3.25.RELEASE.jar!/:4.3.25.RELEASE]
		at org.springframework.transaction.interceptor.TransactionAspectSupport.commitTransactionAfterReturning(TransactionAspectSupport.java:521) ~[spring-tx-4.3.25.RELEASE.jar!/:4.3.25.RELEASE]
		at org.springframework.transaction.interceptor.TransactionAspectSupport.invokeWithinTransaction(TransactionAspectSupport.java:293) ~[spring-tx-4.3.25.RELEASE.jar!/:4.3.25.RELEASE]
		at org.springframework.transaction.interceptor.TransactionInterceptor.invoke(TransactionInterceptor.java:96) ~[spring-tx-4.3.25.RELEASE.jar!/:4.3.25.RELEASE]
		at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:179) ~[spring-aop-4.3.25.RELEASE.jar!/:4.3.25.RELEASE]
		at org.springframework.dao.support.PersistenceExceptionTranslationInterceptor.invoke(PersistenceExceptionTranslationInterceptor.java:136) ~[spring-tx-4.3.25.RELEASE.jar!/:4.3.25.RELEASE]
		at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:179) ~[spring-aop-4.3.25.RELEASE.jar!/:4.3.25.RELEASE]
		at org.springframework.data.jpa.repository.support.CrudMethodMetadataPostProcessor$CrudMethodMetadataPopulatingMethodInterceptor.invoke(CrudMethodMetadataPostProcessor.java:140) ~[spring-data-jpa-1.11.23.RELEASE.jar!/:na]
		at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:179) ~[spring-aop-4.3.25.RELEASE.jar!/:4.3.25.RELEASE]
		at org.springframework.data.jpa.repository.support.CrudMethodMetadataPostProcessor$ExposeRepositoryInvocationInterceptor.invoke(CrudMethodMetadataPostProcessor.java:347) ~[spring-data-jpa-1.11.23.RELEASE.jar!/:na]
		at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:179) ~[spring-aop-4.3.25.RELEASE.jar!/:4.3.25.RELEASE]
		at org.springframework.aop.interceptor.ExposeInvocationInterceptor.invoke(ExposeInvocationInterceptor.java:92) ~[spring-aop-4.3.25.RELEASE.jar!/:4.3.25.RELEASE]
		at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:179) ~[spring-aop-4.3.25.RELEASE.jar!/:4.3.25.RELEASE]
		at org.springframework.data.repository.core.support.SurroundingTransactionDetectorMethodInterceptor.invoke(SurroundingTransactionDetectorMethodInterceptor.java:57) ~[spring-data-commons-1.13.23.RELEASE.jar!/:na]
		at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:179) ~[spring-aop-4.3.25.RELEASE.jar!/:4.3.25.RELEASE]
		at org.springframework.aop.framework.JdkDynamicAopProxy.invoke(JdkDynamicAopProxy.java:213) ~[spring-aop-4.3.25.RELEASE.jar!/:4.3.25.RELEASE]
		at com.sun.proxy.$Proxy226.save(Unknown Source) ~[na:na]
		at dk.digitalidentity.sofd.log.AuditLogger.log(AuditLogger.java:50) ~[classes!/:1.0.0]
		at dk.digitalidentity.sofd.log.AuditInterceptor.interceptViewPerson(AuditInterceptor.java:38) ~[classes!/:1.0.0]
		at sun.reflect.GeneratedMethodAccessor563.invoke(Unknown Source) ~[na:na]
		at sun.reflect.DelegatingMethodAccessorImpl.invoke(Unknown Source) ~[na:1.8.0_282]
		at java.lang.reflect.Method.invoke(Unknown Source) ~[na:1.8.0_282]

	@AfterReturning(pointcut = "execution(* dk.digitalidentity.sofd.controller.mvc.PersonController.view(..))", returning = "retVal")
	public void interceptViewPerson(JoinPoint p, String retVal) {
		if (p.getArgs().length > 1 && p.getArgs()[1] instanceof String) {
			String uuid = (String) p.getArgs()[1];

			auditLogger.log(uuid, EntityType.PERSON, EventType.VIEW_PERSON);
		}
	}
	
	@AfterReturning(pointcut = "execution(* dk.digitalidentity.sofd.controller.mvc.ReportController.getReport(..))", returning = "retVal")
	public void interceptViewReport(JoinPoint p, String retVal) {
		if (p.getArgs().length > 1 && p.getArgs()[1] instanceof ReportType) {
			ReportType reportType = (ReportType) p.getArgs()[1];
		
			auditLogger.log(reportType, EventType.VIEW_REPORT);
		}
	}

	@AfterReturning(pointcut = "execution(* dk.digitalidentity.sofd.controller.mvc.ReportController.downloadReport(..))", returning = "retVal")
	public void interceptDownloadReport(JoinPoint p, ModelAndView retVal) {
		ReportType reportType = (ReportType)p.getArgs()[0];
		auditLogger.log(reportType, EventType.DONWLOAD_REPORT);
	}
	*/
}
