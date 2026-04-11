package dk.digitalidentity.sofd.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@Configuration
public class JdbcTemplateConfiguration {

	@Autowired
	private SofdConfiguration configuration;

	@Bean
	public FlywayMigrationStrategy flywayMigrationStrategy(SofdConfiguration sofdConfiguration) {
		return flyway -> {
			if (sofdConfiguration.isFlywayRepairEnabled()) {
				flyway.repair();
			}
			flyway.migrate();
		};
	}

	@Bean(name = "defaultTemplate")
	public JdbcTemplate defaultTemplate(DataSource dataSource) {
		return new JdbcTemplate(dataSource);
	}

	@Bean(name = "OS2syncTemplate")
	public JdbcTemplate os2syncTemplate() {
		HikariConfig config = new HikariConfig();
		config.setDriverClassName("com.mysql.cj.jdbc.Driver");
		config.setMinimumIdle(0);
		// this ensures it does not try to connect until a connection is needed, so we can do CRaC checkpoint
		config.setInitializationFailTimeout(-1);
		config.setMaximumPoolSize(2);
		config.setJdbcUrl(configuration.getIntegrations().getOs2sync().getDatasourceUrl());
		config.setPassword(configuration.getIntegrations().getOs2sync().getDatasourcePassword());
		config.setUsername(configuration.getIntegrations().getOs2sync().getDatasourceUsername());
		config.setConnectionTimeout(5 * 1000);

		HikariDataSource dataSource = new HikariDataSource(config);

		return new JdbcTemplate(dataSource);
	}
}
