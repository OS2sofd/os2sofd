package dk.digitalidentity.sofd.api.UserTests;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
public class MultipleUsersOnPersonTest {
	private MockMvc mockMvc;

	@Autowired
	private DataGenerator dataGenerator;

	@Autowired
	private WebApplicationContext context;

	@Autowired
	@Qualifier("ApiSecurityFilter")
	private FilterRegistrationBean<ApiSecurityFilter> filterRegistrationBean;

	@Test
	public void patchRemoveUserAndPatchAdd() throws Exception {
		this.mockMvc.perform(get("/api/persons/" + dataGenerator.getUser9Uuid())
				.header("ApiKey", dataGenerator.getApiKey()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.users", hasSize(2)));

		StringBuilder builder = new StringBuilder();
		builder.append("{");
		builder.append("  \"users\" : [{");
		builder.append("    \"uuid\" : \"f78336af-6ef6-400b-bc6d-6a001787ab1f\",");
		builder.append("    \"master\" : \"TEST\",");
		builder.append("    \"masterId\" : \"uniloginid\",");
		builder.append("    \"userId\" : \"uniloginid\",");
		builder.append("    \"userType\" : \"UNILOGIN\"");
		builder.append("  }]");
		builder.append("}");

		this.mockMvc.perform(patch("/api/persons/" + dataGenerator.getUser9Uuid())
				.header("ApiKey", dataGenerator.getApiKey())
				.content(builder.toString()))
				.andExpect(status().isNoContent());

		this.mockMvc.perform(get("/api/persons/" + dataGenerator.getUser9Uuid())
				.header("ApiKey", dataGenerator.getApiKey()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.users", hasSize(1)));

		builder = new StringBuilder();
		builder.append("{");
		builder.append("  \"users\" : [{");
		builder.append("    \"uuid\" : \"f78336af-6ef6-400b-bc6d-6a001787ab1f\",");
		builder.append("    \"master\" : \"TEST\",");
		builder.append("    \"masterId\" : \"uniloginid\",");
		builder.append("    \"userId\" : \"uniloginid\",");
		builder.append("    \"userType\" : \"UNILOGIN\"");
		builder.append("  },{");
		builder.append("    \"uuid\" : \"f78336af-6ef6-400b-bc6d-6a001787ab1e\",");
		builder.append("    \"master\" : \"TEST\",");
		builder.append("    \"masterId\" : \"adlogin\",");
		builder.append("    \"userId\" : \"adlogin\",");
		builder.append("    \"userType\" : \"ACTIVE_DIRECTORY\"");
		builder.append("  },{");
		builder.append("    \"uuid\" : \"f78336af-6ef6-400b-bc6d-6a001787ab1d\",");
		builder.append("    \"master\" : \"TEST\",");
		builder.append("    \"masterId\" : \"opuslogin\",");
		builder.append("    \"userId\" : \"opuslogin\",");
		builder.append("    \"userType\" : \"OPUS\"");
		builder.append("  }]");
		builder.append("}");

		this.mockMvc.perform(patch("/api/persons/" + dataGenerator.getUser9Uuid())
				.header("ApiKey", dataGenerator.getApiKey())
				.content(builder.toString()))
				.andExpect(status().isNoContent());

		this.mockMvc.perform(get("/api/persons/" + dataGenerator.getUser9Uuid())
				.header("ApiKey", dataGenerator.getApiKey()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.users", hasSize(3)));

		builder = new StringBuilder();
		builder.append("{");
		builder.append("  \"users\" : [{");
		builder.append("    \"uuid\" : \"f78336af-6ef6-400b-bc6d-6a001787ab1e\",");
		builder.append("    \"master\" : \"TEST\",");
		builder.append("    \"masterId\" : \"adlogin\",");
		builder.append("    \"userId\" : \"adlogin\",");
		builder.append("    \"userType\" : \"ACTIVE_DIRECTORY\"");
		builder.append("  }]");
		builder.append("}");

		this.mockMvc.perform(patch("/api/persons/" + dataGenerator.getUser9Uuid())
				.header("ApiKey", dataGenerator.getApiKey())
				.content(builder.toString()))
				.andExpect(status().isNoContent());

		this.mockMvc.perform(get("/api/persons/" + dataGenerator.getUser9Uuid())
				.header("ApiKey", dataGenerator.getApiKey()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.users", hasSize(1)));
		
		builder = new StringBuilder();
		builder.append("{");
		builder.append("  \"users\" : [{");
		builder.append("    \"uuid\" : \"f78336af-6ef6-400b-bc6d-6a001787ab1f\",");
		builder.append("    \"master\" : \"TEST\",");
		builder.append("    \"masterId\" : \"uniloginid\",");
		builder.append("    \"userId\" : \"uniloginid\",");
		builder.append("    \"userType\" : \"UNILOGIN\"");
		builder.append("  },{");
		builder.append("    \"uuid\" : \"f78336af-6ef6-400b-bc6d-6a001787ab1e\",");
		builder.append("    \"master\" : \"TEST\",");
		builder.append("    \"masterId\" : \"adlogin\",");
		builder.append("    \"userId\" : \"adlogin\",");
		builder.append("    \"userType\" : \"ACTIVE_DIRECTORY\"");
		builder.append("  },{");
		builder.append("    \"uuid\" : \"f78336af-6ef6-400b-bc6d-6a001787ab1d\",");
		builder.append("    \"master\" : \"TEST\",");
		builder.append("    \"masterId\" : \"opuslogin\",");
		builder.append("    \"userId\" : \"opuslogin\",");
		builder.append("    \"userType\" : \"OPUS\"");
		builder.append("  }]");
		builder.append("}");

		this.mockMvc.perform(patch("/api/persons/" + dataGenerator.getUser9Uuid())
				.header("ApiKey", dataGenerator.getApiKey())
				.content(builder.toString()))
				.andExpect(status().isNoContent());

		this.mockMvc.perform(get("/api/persons/" + dataGenerator.getUser9Uuid())
				.header("ApiKey", dataGenerator.getApiKey()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.users", hasSize(3)));
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
