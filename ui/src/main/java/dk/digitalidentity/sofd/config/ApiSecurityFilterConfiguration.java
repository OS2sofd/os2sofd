package dk.digitalidentity.sofd.config;

import dk.digitalidentity.sofd.service.IpWhitelistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dk.digitalidentity.sofd.security.ApiSecurityFilter;
import dk.digitalidentity.sofd.service.ClientService;

@Configuration
public class ApiSecurityFilterConfiguration {

	@Autowired
	private ClientService clientService;

	@Autowired
	private IpWhitelistService ipWhitelistService;
	
	@Bean(name="ApiSecurityFilter")
	public FilterRegistrationBean<ApiSecurityFilter> apiSecurityFilter() {
		ApiSecurityFilter filter = new ApiSecurityFilter(clientService, ipWhitelistService);

		FilterRegistrationBean<ApiSecurityFilter> filterRegistrationBean = new FilterRegistrationBean<>(filter);
		filterRegistrationBean.addUrlPatterns("/api/*");
		filterRegistrationBean.setOrder(100);
		
		return filterRegistrationBean;
	}
}
