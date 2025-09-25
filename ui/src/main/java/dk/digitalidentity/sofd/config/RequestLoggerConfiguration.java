package dk.digitalidentity.sofd.config;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import javax.servlet.DispatcherType;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dk.digitalidentity.sofd.log.RequestLogger;
import dk.digitalidentity.sofd.log.SecurityLogger;

@Configuration
public class RequestLoggerConfiguration {

	@Bean
	public RequestLogger logFilter(SecurityLogger logger) {
		return new RequestLogger(logger);
	}
	
	@Bean
	public FilterRegistrationBean<RequestLogger> logFilterRegistration(RequestLogger filter) {
		List<String> urlPatterns = new ArrayList<>();
		urlPatterns.add("/api/*");

		FilterRegistrationBean<RequestLogger> registration = new FilterRegistrationBean<>();
		registration.setFilter(filter);
		registration.setDispatcherTypes(EnumSet.allOf(DispatcherType.class));
		registration.setUrlPatterns(urlPatterns);
		
		return registration;
	}
}
