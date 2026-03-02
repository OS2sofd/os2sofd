package dk.digitalidentity.sofd.api.UserTests;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import dk.digitalidentity.sofd.DataGenerator;
import dk.digitalidentity.sofd.security.ApiSecurityFilter;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestPropertySource(locations="classpath:test.properties")
@ActiveProfiles({ "test" })
public class DeleteUserTest {
	private MockMvc mockMvc;

	@Autowired
	private DataGenerator dataGenerator;

	@Autowired
	private WebApplicationContext context;

	@Autowired
	@Qualifier("ApiSecurityFilter")
	private FilterRegistrationBean<ApiSecurityFilter> filterRegistrationBean;

	@Test
	public void deleteUser() throws Exception {
		this.mockMvc.perform(delete("/api/persons/" + dataGenerator.getUser1Uuid())
				.header("ApiKey", dataGenerator.getApiKey()))
				.andExpect(status().isMethodNotAllowed());
	}

	@Before
	public void setUp() {
		dataGenerator.initData();

		// create mockMvc instance, instantiated with the spring context, and with our security filter applied
		this.mockMvc = MockMvcBuilders
				.webAppContextSetup(this.context)
				.addFilter(filterRegistrationBean.getFilter(), "/api/*")
				.build();
	}
}
