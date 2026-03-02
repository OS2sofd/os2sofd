package dk.digitalidentity.sofd.api.UserTests;

import dk.digitalidentity.sofd.DataGenerator;
import dk.digitalidentity.sofd.security.ApiSecurityFilter;

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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestPropertySource(locations="classpath:test.properties")
@ActiveProfiles({ "test" })
public class UserTests {
	private MockMvc mockMvc;

	@Autowired
	private DataGenerator dataGenerator;

	@Autowired
	private WebApplicationContext context;

	@Autowired
	@Qualifier("ApiSecurityFilter")
	private FilterRegistrationBean<ApiSecurityFilter> filterRegistrationBean;

	@Test
	public void listUsers() throws Exception {
		this.mockMvc.perform(get("/api/persons")
				.header("ApiKey", dataGenerator.getApiKey()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.persons", hasSize(10)));
	}

	@Test
	public void getUser2() throws Exception {
		this.mockMvc.perform(get("/api/persons/" + dataGenerator.getUser3Uuid())
				.header("ApiKey", dataGenerator.getApiKey()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.uuid", is(dataGenerator.getUser3Uuid())))
				.andExpect(jsonPath("$.deleted", is(false)))
				.andExpect(jsonPath("$.firstname", is("Gert")))
				.andExpect(jsonPath("$.surname", is("Gunnerson")))
				.andExpect(jsonPath("$.affiliations", hasSize(1)))
				.andExpect(jsonPath("$.phones", hasSize(2)))
				.andExpect(jsonPath("$.registeredPostAddress.country", is("DK")));
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
