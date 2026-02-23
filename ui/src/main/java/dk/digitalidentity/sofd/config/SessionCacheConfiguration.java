package dk.digitalidentity.sofd.config;

import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.session.jdbc.MySqlJdbcIndexedSessionRepositoryCustomizer;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;
import org.springframework.session.jdbc.config.annotation.web.http.JdbcHttpSessionConfiguration;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.session.web.http.SessionRepositoryFilter;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;

@Configuration
@EnableJdbcHttpSession(maxInactiveIntervalInSeconds = 14400) // 4 hours
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
		serializer.setCookieMaxAge(4 * 60 * 60);

		return serializer;
	}

	// Spring Session JDBC optimizations for MySQL
	@Bean
	public MySqlJdbcIndexedSessionRepositoryCustomizer sessionRepositoryCustomizer() {
		return new MySqlJdbcIndexedSessionRepositoryCustomizer();
	}

	// we have to disable the build-in Spring Sessions filter
    @Bean
    public FilterRegistrationBean<SessionRepositoryFilter<?>> disableAutoSessionFilter(SessionRepositoryFilter<?> filter) {
        FilterRegistrationBean<SessionRepositoryFilter<?>> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);

        return reg;
    }

    // and then build our own, that excludes static resources
    @Bean
    public FilterRegistrationBean<Filter> sessionFilterWithExclusions(SessionRepositoryFilter<?> sessionRepositoryFilter) {
        FilterRegistrationBean<Filter> reg = new FilterRegistrationBean<>();

        reg.setFilter((request, response, chain) -> {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            String path = httpRequest.getRequestURI();

            if (isStaticResource(path)) {
                chain.doFilter(request, response);
            }
            else {
                sessionRepositoryFilter.doFilter(request, response, chain);
            }
        });

        reg.setName("springSessionRepositoryFilter");
        reg.setOrder(Integer.MIN_VALUE + 50);
        reg.addUrlPatterns("/*");
        return reg;
    }

    // and finally we need to tell Spring Security that it these resources are stateless (no session please)
    @Bean
    @Order(0)
    public SecurityFilterChain staticResourcesFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher(
            	"/css/**",
            	"/js/**",
            	"/img/**",
            	"/webjars/**",
            	"/favicon.ico",
                "/**/*.js.map",
                "/**/*.css.map"
            )
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .csrf(csrf -> csrf.disable())
            .securityContext(ctx -> ctx.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .requestCache(cache -> cache.disable());

        return http.build();
    }

	private boolean isStaticResource(String path) {
        return path.startsWith("/css/") ||
        	   path.startsWith("/js/") ||
        	   path.startsWith("/img/") ||
        	   path.startsWith("/webjars/") ||
        	   path.equals("/favicon.ico") ||
        	   path.endsWith(".js.map") ||
        	   path.endsWith(".css.map");
    }
}
