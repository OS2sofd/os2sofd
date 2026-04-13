package dk.digitalidentity.sofd.config;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.stereotype.Component;

@Component
public class CracBeanConfiguration implements BeanFactoryPostProcessor {

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		// remove HikariCP checkpoint/restore lifecycle - we handle this manually
		if (beanFactory instanceof BeanDefinitionRegistry registry) {
			if (beanFactory.containsBeanDefinition("hikariCheckpointRestoreLifecycle")) {
				registry.removeBeanDefinition("hikariCheckpointRestoreLifecycle");
			}
		}

		// ensure S3Autoconfiguration is lazy, as it is not available at build-time
		for (String name : beanFactory.getBeanDefinitionNames()) {
			BeanDefinition bd = beanFactory.getBeanDefinition(name);
			String factoryBeanName = bd.getFactoryBeanName();

			if (factoryBeanName != null && factoryBeanName.contains("S3AutoConfiguration")) {
				bd.setLazyInit(true);
			}
		}
	}
}
