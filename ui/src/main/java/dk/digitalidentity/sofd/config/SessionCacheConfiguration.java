package dk.digitalidentity.sofd.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.session.jdbc.MySqlJdbcIndexedSessionRepositoryCustomizer;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;
import org.springframework.session.jdbc.config.annotation.web.http.JdbcHttpSessionConfiguration;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

import javax.annotation.PostConstruct;
import java.util.Random;

@Configuration
@EnableJdbcHttpSession
public class SessionCacheConfiguration {

	@Autowired
	private JdbcHttpSessionConfiguration sessionConfiguration;

	@Autowired
	private SofdConfiguration sofdConfiguration;

	// cleanup Spring Sessions JDB every 10 minutes with a bit of fuzz
	// run only on master instance and and spread load across customers
	// note that this grabs an SQL connection
	@PostConstruct
	public void configureCleanup() {
		if (sofdConfiguration.getScheduled().isEnabled()) {
			// master instance: run cleanup with randomized schedule
			Random random = new Random();
			int seconds = random.nextInt(60);  // 0-59
			int minuteStart = random.nextInt(10);  // 0-9

			String cleanupCron = String.format("%d %d/10 * * * *", seconds, minuteStart);
			sessionConfiguration.setCleanupCron(cleanupCron);
		} else {
			// slave instance: disable cleanup
			sessionConfiguration.setCleanupCron(Scheduled.CRON_DISABLED);
		}
	}

	@Bean
	public CookieSerializer cookieSerializer() {
		DefaultCookieSerializer serializer = new DefaultCookieSerializer();
		serializer.setCookieName("JSESSIONID");
		serializer.setCookiePath("/");
		serializer.setUseSecureCookie(true);
		serializer.setSameSite("None");
		serializer.setCookieMaxAge(8 * 60 * 60);

		return serializer;
	}

	// Spring Session JDBC optimizations for MySQL
	@Bean
	public MySqlJdbcIndexedSessionRepositoryCustomizer sessionRepositoryCustomizer() {
		return new MySqlJdbcIndexedSessionRepositoryCustomizer();
	}
}
