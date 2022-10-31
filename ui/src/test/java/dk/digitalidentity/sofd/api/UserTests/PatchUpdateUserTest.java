package dk.digitalidentity.sofd.api.UserTests;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.hamcrest.core.IsNull;
import org.json.JSONObject;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import dk.digitalidentity.sofd.DataGenerator;
import dk.digitalidentity.sofd.security.ApiSecurityFilter;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestPropertySource(locations="classpath:test.properties")
@ActiveProfiles({ "test" })
public class PatchUpdateUserTest {
	private MockMvc mockMvc;

	@Autowired
	private DataGenerator dataGenerator;

	@Autowired
	private WebApplicationContext context;

	@Autowired
	@Qualifier("ApiSecurityFilter")
	private FilterRegistrationBean<ApiSecurityFilter> filterRegistrationBean;

	@Test
	public void validPatchUpdateOnUser() throws Exception{
		StringBuilder builder = new StringBuilder();
		builder.append("{");
		builder.append("  \"registeredPostAddress\" : {");
		builder.append("    \"street\" : \"Hasselager Centervej 15\",");
		builder.append("    \"localname\" : \"Hasselager\",");
		builder.append("    \"postalCode\" : \"8261\",");
		builder.append("    \"city\" : \"Viby J\",");
		builder.append("    \"country\" : \"Danmark\",");
		builder.append("    \"addressProtected\" : false,");
		builder.append("    \"prime\": true");
		builder.append("  },");
		builder.append("  \"chosenName\" : null");
		builder.append("}");

		this.mockMvc.perform(patch("/api/persons/" + dataGenerator.getUser1Uuid())
				.header("ApiKey", dataGenerator.getApiKey())
				.content(builder.toString()))
				.andExpect(status().isNoContent());

		this.mockMvc.perform(get("/api/persons/" + dataGenerator.getUser1Uuid())
				.header("ApiKey", dataGenerator.getApiKey()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.registeredPostAddress.postalCode", is("8261")))
				.andExpect(jsonPath("$.chosenName").value(IsNull.nullValue()));
	}

	@Test
	public void validPatchUpdateOnNonExistingUser() throws Exception{
		String uuid = UUID.randomUUID().toString();

		StringBuilder builder = new StringBuilder();
		builder.append("{");
		builder.append("  \"registeredPostAddress\" : {");
		builder.append("    \"street\" : \"Hasselager Centervej 15\",");
		builder.append("    \"localname\" : \"Hasselager\",");
		builder.append("    \"postalCode\" : \"8261\",");
		builder.append("    \"city\" : \"Viby J\",");
		builder.append("    \"country\" : \"Danmark\",");
		builder.append("    \"addressProtected\" : false,");
		builder.append("    \"prime\": true");
		builder.append("  }");
		builder.append("}");

		this.mockMvc.perform(patch("/api/persons/" + uuid)
				.header("ApiKey", dataGenerator.getApiKey())
				.content(builder.toString()))
				.andExpect(status().isNotFound());
	}

	@Test
	public void invalidPatchUpdateOnUser() throws Exception{
		StringBuilder builder = new StringBuilder();
		builder.append("{");
		builder.append("  \"cpr\" : null");
		builder.append("}");

		this.mockMvc.perform(patch("/api/persons/" + dataGenerator.getUser1Uuid())
				.header("ApiKey", dataGenerator.getApiKey())
				.content(builder.toString()))
				.andExpect(status().isBadRequest());
	}

	@Test
	public void PatchUpdateLocalExtensionsOnUser() throws Exception{
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

		this.mockMvc.perform(patch("/api/persons/" + dataGenerator.getUser1Uuid())
				.header("ApiKey", dataGenerator.getApiKey())
				.content(builder.toString()))
				.andExpect(status().isNoContent());

		this.mockMvc.perform(get("/api/persons/" + dataGenerator.getUser1Uuid())
				.header("ApiKey", dataGenerator.getApiKey()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.localExtensions.keyCardId", is(73184)))
				.andExpect(jsonPath("$.localExtensions.shoeSize", is("43 EU")))
				.andExpect(jsonPath("$.localExtensions.lederOplysninger.afdeling", is("IT og digitalisering")))
				.andExpect(jsonPath("$.localExtensions.lederOplysninger.medarbejederansvar", is(false)));
	}

	@Test
	public void PatchUpdateEmploymentOnUserRemove() throws Exception{
		StringBuilder builder = new StringBuilder();
		builder.append("{");
		builder.append("	\"affiliations\" : []");
		builder.append("}");

		this.mockMvc.perform(patch("/api/persons/" + dataGenerator.getUser1Uuid())
				.header("ApiKey", dataGenerator.getApiKey())
				.content(builder.toString()))
				.andExpect(status().isNoContent());

		this.mockMvc.perform(get("/api/persons/" + dataGenerator.getUser1Uuid())
				.header("ApiKey", dataGenerator.getApiKey()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.affiliations", hasSize(0)));
	}

	@Test
	public void PatchUpdateEmploymentOnUserGive() throws Exception{
        String uuid = UUID.randomUUID().toString();

		StringBuilder builder = new StringBuilder();
		builder.append("{");
		builder.append("  \"uuid\" : \"" + uuid + "\",");
		builder.append("  \"cpr\" : \"1234567890\",");
		builder.append("  \"master\" : \"TEST\",");
		builder.append("  \"masterId\" : \"" + uuid + "\",");
		builder.append("  \"firstname\" : \"firstname\",");
		builder.append("  \"surname\" : \"surname\",");
		builder.append("  \"registeredPostAddress\" : {");
		builder.append("    \"street\" : \"Hasselager Centervej 15\",");
		builder.append("    \"localname\" : \"Hasselager\",");
		builder.append("    \"postalCode\" : \"8261\",");
		builder.append("    \"city\" : \"Viby J\",");
		builder.append("    \"country\" : \"Danmark\",");
		builder.append("    \"addressProtected\" : false,");
		builder.append("    \"master\" : \"TEST\",");
		builder.append("    \"masterId\" : \"TEST\",");
		builder.append("    \"prime\": true");
		builder.append("  }");
		builder.append("}");

        this.mockMvc.perform(post("/api/persons")
                .header("ApiKey", dataGenerator.getApiKey())
                .content(builder.toString()))
                .andExpect(status().isCreated());

        ResultActions resultActionsOrgUnit = this.mockMvc.perform(get("/api/orgUnits/" + dataGenerator.getOuSundhedUuid())
                .header("ApiKey", dataGenerator.getApiKey()));

        JSONObject jsonObjOrgUnit = new JSONObject(resultActionsOrgUnit.andReturn().getResponse().getContentAsString());

        JSONObject linksObj = (JSONObject) jsonObjOrgUnit.get("_links");
        JSONObject selfObj = (JSONObject) linksObj.get("self");

        String orgUnitHref = selfObj.get("href").toString();

        builder = new StringBuilder();
        builder.append("{");
		builder.append("  \"affiliations\" : [{");
		builder.append("    \"orgUnit\": \"" + orgUnitHref + "\",");
		builder.append("    \"master\" : \"TEST\",");
		builder.append("    \"masterId\" : \"401\",");
		builder.append("    \"employeeId\": \"401\",");
		builder.append("    \"affiliationType\": \"EMPLOYEE\",");
		builder.append("    \"positionId\":  \"123\",");
		builder.append("    \"positionName\": \"Ansat\",");
		builder.append("    \"localExtensions\" : { \"key\": \"value\" }");
		builder.append("  }]");
        builder.append("}");

        this.mockMvc.perform(patch("/api/persons/" + uuid)
                .header("ApiKey", dataGenerator.getApiKey())
                .content(builder.toString()))
                .andExpect(status().isNoContent());

        this.mockMvc.perform(get("/api/persons/" + uuid)
                .header("ApiKey", dataGenerator.getApiKey()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.affiliations", hasSize(1)));
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
