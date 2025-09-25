package dk.digitalidentity.sofd.api.orgUnitTests;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.hamcrest.core.IsNull;
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
public class PatchUpdateOrgUnitTests {
	private MockMvc mockMvc;

	@Autowired
	private DataGenerator dataGenerator;

	@Autowired
	private WebApplicationContext context;

	@Autowired
	@Qualifier("ApiSecurityFilter")
	private FilterRegistrationBean<ApiSecurityFilter> filterRegistrationBean;

	@Test
	public void validPatchUpdateOnOrgUnit() throws Exception{
		StringBuilder builder = new StringBuilder();
		builder.append("{");
		builder.append("  \"name\" : \"HR test\",");
		builder.append("  \"pnr\" : 7891,");
		builder.append("  \"ean\" : null");
		builder.append("}");

		this.mockMvc.perform(patch("/api/orgUnits/" + dataGenerator.getOuHRUuid())
				.header("ApiKey", dataGenerator.getApiKey())
				.content(builder.toString()))
				.andExpect(status().isNoContent());

		this.mockMvc.perform(get("/api/orgUnits/" + dataGenerator.getOuHRUuid())
				.header("ApiKey", dataGenerator.getApiKey()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.ean", is(IsNull.nullValue())))
				.andExpect(jsonPath("$.name", is("HR test")))
				.andExpect(jsonPath("$.pnr", is(7891)));
	}

	@Test
	public void validPatchUpdateOnNonExistingOrgUnit() throws Exception{

		StringBuilder builder = new StringBuilder();
		builder.append("{");
		builder.append("  \"name\" : \"HR test\",");
		builder.append("  \"pnr\" : 7891,");
		builder.append("  \"ean\" : null");
		builder.append("}");

		this.mockMvc.perform(patch("/api/orgUnits/" + UUID.randomUUID().toString())
				.header("ApiKey", dataGenerator.getApiKey())
				.content(builder.toString()))
				.andExpect(status().isNotFound());
	}

	@Test
	public void invalidPatchUpdateOnOrgUnit() throws Exception{
		StringBuilder builder = new StringBuilder();
		builder.append("{");
		builder.append("  \"name\" : null");
		builder.append("}");

		this.mockMvc.perform(patch("/api/orgUnits/" + dataGenerator.getOuHRUuid())
				.header("ApiKey", dataGenerator.getApiKey())
				.content(builder.toString()))
				.andExpect(status().isBadRequest());
	}

	@Test
	public void PatchUpdateLocalExtensionsOnOrgUnit() throws Exception{
		StringBuilder builder = new StringBuilder();
		builder.append("{");
		builder.append("  \"localExtensions\" : {");
		builder.append("    \"keyCardId\" : 73184,");
		builder.append("    \"shoeSize\" : \"43 EU\",");
		builder.append("    \"lederOplysninger\" : {");
		builder.append("    	\"afdeling\" : \"IT og digitalisering\",");
		builder.append("    	\"medarbejederansvar\" : false");
		builder.append("  	}");
		builder.append("  }");
		builder.append("}");

		this.mockMvc.perform(patch("/api/orgUnits/" + dataGenerator.getOuHRUuid())
				.header("ApiKey", dataGenerator.getApiKey())
				.content(builder.toString()))
				.andExpect(status().isNoContent());

		this.mockMvc.perform(get("/api/orgUnits/" + dataGenerator.getOuHRUuid())
				.header("ApiKey", dataGenerator.getApiKey()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.localExtensions.keyCardId", is(73184)))
				.andExpect(jsonPath("$.localExtensions.shoeSize", is("43 EU")))
				.andExpect(jsonPath("$.localExtensions.lederOplysninger.afdeling", is("IT og digitalisering")))
				.andExpect(jsonPath("$.localExtensions.lederOplysninger.medarbejederansvar", is(false)));
	}

	@Test
	public void PatchUpdateEngangementOnOrgUnitRemove() throws Exception{
		StringBuilder builder = new StringBuilder();
		builder.append("{");
		builder.append("  \"affiliations\" : []");
		builder.append("}");

		this.mockMvc.perform(patch("/api/orgUnits/" + dataGenerator.getOuHRUuid())
				.header("ApiKey", dataGenerator.getApiKey())
				.content(builder.toString()))
				.andExpect(status().isNoContent());

		this.mockMvc.perform(get("/api/orgUnits/" + dataGenerator.getOuHRUuid())
				.header("ApiKey", dataGenerator.getApiKey()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.affiliations", hasSize(2)));
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
