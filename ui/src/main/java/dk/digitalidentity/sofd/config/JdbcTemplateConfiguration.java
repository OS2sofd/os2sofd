package dk.digitalidentity.sofd.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@Configuration
public class JdbcTemplateConfiguration {

	@Autowired
	private SofdConfiguration configuration;

	@Bean(name = "defaultTemplate")
	public JdbcTemplate defaultTemplate(DataSource dataSource) {
		return new JdbcTemplate(dataSource);
	}

	@Bean(name = "OS2syncTemplate")
	public JdbcTemplate os2syncTemplate() {
		HikariConfig config = new HikariConfig();
		config.setConnectionTestQuery("SELECT 1");
		config.setDriverClassName("com.mysql.cj.jdbc.Driver");
		config.setMinimumIdle(1);
		config.setMaximumPoolSize(5);
		config.setJdbcUrl(configuration.getIntegrations().getOs2sync().getDatasourceUrl());
		config.setPassword(configuration.getIntegrations().getOs2sync().getDatasourcePassword());
		config.setUsername(configuration.getIntegrations().getOs2sync().getDatasourceUsername());
		config.setConnectionTimeout(5 * 1000);

		HikariDataSource dataSource = new HikariDataSource(config);

		return new JdbcTemplate(dataSource);
	}
}
