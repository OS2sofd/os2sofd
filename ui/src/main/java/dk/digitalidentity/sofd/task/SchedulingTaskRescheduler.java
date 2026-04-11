package dk.digitalidentity.sofd.task;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.expression.StandardBeanExpressionResolver;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.config.Task;
import org.springframework.scheduling.support.ScheduledMethodRunnable;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SchedulingTaskRescheduler implements SchedulingConfigurer {
    private ScheduledTaskRegistrar registrar;

    @Autowired
    private ScheduledTaskHolder scheduledTaskHolder;

    @Autowired
    private BeanFactory beanFactory;
    
    @Autowired
    private Environment environment;

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        this.registrar = registrar;
    }
    
    // when using CRaC, all of our randomness gets applied during checkpoint, which means that all restored instances will have the
    // exact same randomness applied. This is unwanted, so we re-apply the fuzz to all methods that have a @Reschedule annotation
    public void addFuzz() {
    	for (ScheduledTask scheduledTask : scheduledTaskHolder.getScheduledTasks()) {
        	// only care about those with a CRON expression, as they are the ones that needs random fuzz
            Task task = scheduledTask.getTask();
            if (!(task instanceof CronTask)) {
            	log.trace("Ignoring non-cron");
            	continue;
            }

            Runnable runnable = task.getRunnable();
            ScheduledMethodRunnable smr = unwrapRunnable(runnable);
            if (smr == null) {
            	log.trace("Could not unrap runnable");
            	continue;
            }

			Method method = smr.getMethod();
			if (method.isAnnotationPresent(Reschedule.class)) {
				Reschedule info = method.getAnnotation(Reschedule.class);

				String newCron = resolveExpression(info.cron());
	            log.debug("Setting " + method.getName() + " to cron: " + newCron);

	            scheduledTask.cancel();

	            CronTask newCronTask = new CronTask(runnable, newCron);
	            
	            registrar.scheduleCronTask(newCronTask);
			}
    	}
    }

    // spring wraps our runnable in some auto-generated wrapper, so we need to unwrap till we hit the ScheduledMethodRunnable
    private ScheduledMethodRunnable unwrapRunnable(Runnable runnable) {
        if (runnable instanceof ScheduledMethodRunnable smr) {
            return smr;
        }

        try {
            Field delegateField = runnable.getClass().getDeclaredField("runnable");
            delegateField.setAccessible(true);
            Object delegate = delegateField.get(runnable);

            if (delegate instanceof ScheduledMethodRunnable smr) {
                return smr;
            }

            if (delegate instanceof Runnable nestedRunnable) {
                return unwrapRunnable(nestedRunnable);
            }
        }
        catch (NoSuchFieldException | IllegalAccessException ex) {
            log.warn("Could not unwrap runnable of type {}", runnable.getClass().getName(), ex);
        }

        return null;
    }
    
    // this method allows us to expand #{new java.util.Random().nextInt(60)} into something meaningful (it also supports placeholders)
    private String resolveExpression(String cron) {
        String resolved = environment.resolvePlaceholders(cron);
        
        StandardBeanExpressionResolver resolver = new StandardBeanExpressionResolver();
        BeanExpressionContext context = new BeanExpressionContext((ConfigurableBeanFactory) beanFactory, null);
        Object evaluated = resolver.evaluate(resolved, context);
        
        return evaluated != null ? evaluated.toString() : resolved;
    }
}
