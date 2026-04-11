package dk.digitalidentity.sofd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.micrometer.observation.autoconfigure.ObservationAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication(
	scanBasePackages = "dk.digitalidentity",
	exclude = {
	    MetricsAutoConfiguration.class,
	    SimpleMetricsExportAutoConfiguration.class,
	    CompositeMeterRegistryAutoConfiguration.class,
	    ObservationAutoConfiguration.class
	})
public class SofdCoreApplication {

	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(SofdCoreApplication.class, args);
		context.start();
	}
}
