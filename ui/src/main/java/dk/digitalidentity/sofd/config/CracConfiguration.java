package dk.digitalidentity.sofd.config;

import org.springframework.boot.jdbc.HikariCheckpointRestoreLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.activation.DataSource;

@Configuration
public class CracConfiguration {

	@Bean
	public HikariCheckpointRestoreLifecycle hikariCheckpointRestoreLifecycle(DataSource dataSource) {
	    // disable the build-in lifecycle during checkpoint/restore, as suspending is not enough for us
	    return null;
	}
}
