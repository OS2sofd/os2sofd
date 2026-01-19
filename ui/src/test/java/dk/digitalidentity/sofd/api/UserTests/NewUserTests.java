package dk.digitalidentity.sofd.api.UserTests;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.UUID;

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
import net.minidev.json.JSONArray;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestPropertySource(locations="classpath:test.properties")
@ActiveProfiles({ "test" })
public class NewUserTests {
	private MockMvc mockMvc;

	@Autowired
	private DataGenerator dataGenerator;

	@Autowired
	private WebApplicationContext context;

	@Autowired
	@Qualifier("ApiSecurityFilter")
	private FilterRegistrationBean<ApiSecurityFilter> filterRegistrationBean;

	@Test
	public void createUserAndTestExists() throws Exception {
		String uuid = UUID.randomUUID().toString();
		String uuid2 = UUID.randomUUID().toString();
		
		ResultActions resultActions = this.mockMvc.perform(get("/api/orgUnits/" + dataGenerator.getOuHRUuid()).header("ApiKey", dataGenerator.getApiKey()));
		JSONObject jsonObj = new JSONObject(resultActions.andReturn().getResponse().getContentAsString());
		JSONObject linksObj = (JSONObject) jsonObj.get("_links");
		JSONObject selfObj = (JSONObject) linksObj.get("self");
		String orgUnitHref = selfObj.get("href").toString();

		StringBuilder builder = new StringBuilder();
		builder.append("{");
		builder.append("  \"uuid\" : \"" + uuid + "\",");
		builder.append("  \"master\" : \"TEST\",");
		builder.append("  \"masterId\" : \"" + uuid + "\",");
		builder.append("  \"users\" : [{");
		builder.append("    \"userId\" : \"user234\",");
		builder.append("    \"uuid\" : \"" + uuid2 + "\",");
		builder.append("    \"master\" : \"TEST\",");
		builder.append("    \"masterId\" : \"user234\",");
		builder.append("    \"userType\" : \"ACTIVE_DIRECTORY\",");
		builder.append("      \"email\" : {");
		builder.append("        \"email\" : \"hello@mail.dk\",");
		builder.append("        \"master\" : \"TEST\",");
		builder.append("        \"masterId\" : \"TEST\",");
		builder.append("        \"prime\": true");
		builder.append("      },");
		builder.append("    \"localExtensions\" : { \"key\": \"value\" }");
		builder.append("  }],");
		builder.append("  \"cpr\" : \"1234567890\",");
		builder.append("  \"firstname\" : \"kkk\",");
		builder.append("  \"surname\" : \"abc\",");
		builder.append("  \"chosenName\" : \"xyz\",");
		builder.append("  \"registeredPostAddress\" : {");
		builder.append("    \"street\" : \"Hasselager Centervej 17\",");
		builder.append("    \"localname\" : \"Hasselager\",");
		builder.append("    \"postalCode\" : \"8260\",");
		builder.append("    \"city\" : \"Viby J\",");
		builder.append("    \"country\" : \"Danmark\",");
		builder.append("    \"addressProtected\" : false,");
		builder.append("    \"master\" : \"TEST\",");
		builder.append("    \"masterId\" : \"TEST\",");
		builder.append("    \"prime\": true");
		builder.append("  },");
		builder.append("  \"residencePostAddress\" : {");
		builder.append("    \"street\" : \"Skovsgårdsvænget 15\",");
		builder.append("    \"localname\" : \"Derhjemme\",");
		builder.append("    \"postalCode\" : \"8362\",");
		builder.append("    \"city\" : \"Hørning\",");
		builder.append("    \"country\" : \"Danmark\",");
		builder.append("    \"addressProtected\" : true,");
		builder.append("    \"master\" : \"TEST\",");
		builder.append("    \"masterId\" : \"TEST\",");
		builder.append("    \"prime\": false");
		builder.append("  },");
		builder.append("  \"phones\" : [{");
		builder.append("    \"phoneNumber\" : \"22446688\",");
		builder.append("    \"prime\": true,");
		builder.append("    \"master\" : \"TEST\",");
		builder.append("    \"masterId\" : \"TEST\",");
		builder.append("    \"phoneType\" : \"MOBILE\"");
		builder.append("  },{");
		builder.append("    \"phoneNumber\" : \"11335577\",");
		builder.append("    \"prime\": false,");
		builder.append("    \"master\" : \"TEST\",");
		builder.append("    \"masterId\" : \"TEST\",");
		builder.append("    \"phoneType\" : \"LANDLINE\"");
		builder.append("  }],");
		builder.append("  \"localExtensions\" : { \"key\": \"value\" },");
		builder.append("  \"affiliations\" : [{");
		builder.append("    \"orgUnit\": \"" + orgUnitHref + "\",");
		builder.append("    \"employeeId\": \"401\",");
		builder.append("    \"master\" : \"TEST\",");
		builder.append("    \"masterId\" : \"401\",");
		builder.append("    \"affiliationType\": \"EMPLOYEE\",");
		builder.append("    \"positionId\": \"123\",");
		builder.append("    \"positionName\": \"Ansat\",");
		builder.append("    \"localExtensions\" : { \"key\": \"value\" }");
		builder.append("  }]");
		builder.append("}");

		this.mockMvc.perform(post("/api/persons")
				.header("ApiKey", dataGenerator.getApiKey())
				.content(builder.toString()))
				.andExpect(status().isCreated());

		this.mockMvc.perform(get("/api/persons")
				.header("ApiKey", dataGenerator.getApiKey()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.persons", hasSize(11)));

		this.mockMvc.perform(get("/api/persons/" + uuid)
				.header("ApiKey", dataGenerator.getApiKey()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.uuid", is(uuid)))
				.andExpect(jsonPath("$.deleted", is(false)))
				.andExpect(jsonPath("$.firstname", is("kkk")))
				.andExpect(jsonPath("$.surname", is("abc")))
				.andExpect(jsonPath("$.chosenName", is("xyz")))
				.andExpect(jsonPath("$.cpr", is("1234567890")))
				.andExpect(jsonPath("$.residencePostAddress.country", is("Danmark")))
				.andExpect(jsonPath("$.residencePostAddress.prime", is(false)))
				.andExpect(jsonPath("$.phones", hasSize(2)))
				.andExpect(jsonPath("$.affiliations", hasSize(1)))
				.andExpect(jsonPath("$.users[0].userId", is("user234")));
	}

	@Test
	public void createUserWithMandatoryFieldsOnly() throws Exception{
		String uuid = UUID.randomUUID().toString();

		StringBuilder builder = new StringBuilder();
		builder.append("{");
		builder.append("  \"uuid\" : \"" + uuid + "\",");
		builder.append("  \"cpr\" : \"1234567890\",");
		builder.append("  \"firstname\" : \"firstname\",");
		builder.append("  \"surname\" : \"surname\",");
		builder.append("  \"registeredPostAddress\" : {");
		builder.append("    \"street\" : \"Hasselager Centervej 17\",");
		builder.append("    \"localname\" : \"Hasselager\",");
		builder.append("    \"postalCode\" : \"8260\",");
		builder.append("    \"city\" : \"Viby J\",");
		builder.append("    \"country\" : \"Danmark\",");
		builder.append("    \"addressProtected\" : false,");
		builder.append("    \"master\" : \"TEST\",");
		builder.append("    \"masterId\" : \"TEST\",");
		builder.append("    \"prime\": true");
		builder.append("  },");
		builder.append("  \"phones\" : [{");
		builder.append("    \"phoneNumber\" : \"22446688\",");
		builder.append("    \"prime\": true,");
		builder.append("    \"phoneType\" : \"MOBILE\"");
		builder.append("  },{");
		builder.append("    \"phoneNumber\" : \"11335577\",");
		builder.append("    \"prime\": true,");
		builder.append("    \"phoneType\" : \"LANDLINE\"");
		builder.append("  }]");
		builder.append("}");

		this.mockMvc.perform(post("/api/persons")
				.header("ApiKey", dataGenerator.getApiKey())
				.content(builder.toString()))
				.andExpect(status().isBadRequest());
	}

	@Test
	public void testCreateUserWith2PrimePhones() throws Exception{
		String uuid = UUID.randomUUID().toString();

		StringBuilder builder = new StringBuilder();
		builder.append("{");
		builder.append("  \"uuid\" : \"" + uuid + "\",");
		builder.append("  \"master\" : \"TEST\",");
		builder.append("  \"masterId\" : \"" + uuid + "\",");
		builder.append("  \"cpr\" : \"1234567890\",");
		builder.append("  \"firstname\" : \"firstname\",");
		builder.append("  \"surname\" : \"surname\",");
		builder.append("  \"registeredPostAddress\" : {");
		builder.append("    \"street\" : \"Hasselager Centervej 17\",");
		builder.append("    \"localname\" : \"Hasselager\",");
		builder.append("    \"postalCode\" : \"8260\",");
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

		this.mockMvc.perform(get("/api/persons/" + uuid)
				.header("ApiKey", dataGenerator.getApiKey()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.uuid", is(uuid)))
				.andExpect(jsonPath("$.deleted", is(false)));
	}
	
	@Test
	public void createUserWithInvalidInputAndVerifyCallFailing() throws Exception{
		String uuid = UUID.randomUUID().toString();

		StringBuilder builder = new StringBuilder();
		builder.append("{");
		builder.append("  \"uuid\" : \"" + uuid + "\",");
		builder.append("  \"firstname\" : \"Test\",");
		builder.append("  \"surname\" : 1234");
		builder.append("}");

		this.mockMvc.perform(post("/api/persons")
				.header("ApiKey", dataGenerator.getApiKey())
				.content(builder.toString()))
				.andExpect(status().isBadRequest());
	}

	@Test
	public void createAndVerifyLocalExtensionsOnUser() throws Exception{
		String uuid = UUID.randomUUID().toString();

		StringBuilder builder = new StringBuilder();
		builder.append("{");
		builder.append("  \"uuid\" : \"" + uuid + "\",");
		builder.append("  \"master\" : \"TEST\",");
		builder.append("  \"masterId\" : \"" + uuid + "\",");
		builder.append("  \"cpr\" : \"1234567890\",");
		builder.append("  \"firstname\" : \"firstname\",");
		builder.append("  \"surname\" : \"surname\",");
		builder.append("  \"registeredPostAddress\" : {");
		builder.append("    \"street\" : \"Hasselager Centervej 17\",");
		builder.append("    \"localname\" : \"Hasselager\",");
		builder.append("    \"postalCode\" : \"8260\",");
		builder.append("    \"city\" : \"Viby J\",");
		builder.append("    \"country\" : \"Danmark\",");
		builder.append("    \"addressProtected\" : false,");
		builder.append("    \"master\" : \"TEST\",");
		builder.append("    \"masterId\" : \"TEST\",");
		builder.append("    \"prime\": true");
		builder.append("  },");
		builder.append("  \"localExtensions\" : {");
		builder.append("    \"keyCardId\" : 73184,");
		builder.append("    \"shoeSize\" : \"43 EU\",");
		builder.append("    \"lederOplysninger\" : {");
		builder.append("    	\"afdeling\" : \"IT og digitalisering\",");
		builder.append("    	\"medarbejederansvar\" : false");
		builder.append("  	}");
		builder.append("  }");
		builder.append("}");

		this.mockMvc.perform(post("/api/persons")
				.header("ApiKey", dataGenerator.getApiKey())
				.content(builder.toString()))
				.andExpect(status().isCreated());

		this.mockMvc.perform(get("/api/persons/" + uuid)
				.header("ApiKey", dataGenerator.getApiKey()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.uuid", is(uuid)))
				.andExpect(jsonPath("$.localExtensions.keyCardId", is(73184)))
				.andExpect(jsonPath("$.localExtensions.shoeSize", is("43 EU")))
				.andExpect(jsonPath("$.localExtensions.lederOplysninger.afdeling", is("IT og digitalisering")))
				.andExpect(jsonPath("$.localExtensions.lederOplysninger.medarbejederansvar", is(false)));
	}

	@Test
	public void createAndVerifyUserWithOneEmplyoment() throws Exception{
		String uuid = UUID.randomUUID().toString();

		ResultActions resultActions = this.mockMvc.perform(get("/api/orgUnits/" + dataGenerator.getOuHRUuid())
				.header("ApiKey", dataGenerator.getApiKey()));

		JSONObject jsonObj = new JSONObject(resultActions.andReturn().getResponse().getContentAsString());

		JSONObject linksObj = (JSONObject) jsonObj.get("_links");
		JSONObject selfObj = (JSONObject) linksObj.get("self");

		String orgUnitHref = selfObj.get("href").toString();

		/* Workaround for when the projections are added to the project */
		JSONArray orgUnitHrefExpected = new JSONArray();
		orgUnitHrefExpected.add(orgUnitHref + "{?projection}");

		StringBuilder builder = new StringBuilder();
		builder.append("{");
		builder.append("  \"uuid\" : \"" + uuid + "\",");
		builder.append("  \"master\" : \"TEST\",");
		builder.append("  \"masterId\" : \"" + uuid + "\",");
		builder.append("  \"user\" : {");
		builder.append("    \"userType\" : \"ACTIVE_DIRECTORY\",");
		builder.append("    \"master\" : \"TEST\",");
		builder.append("    \"masterId\" : \"user234\",");
		builder.append("    \"userId\" : \"user234\"");
		builder.append("  },");
		builder.append("  \"cpr\" : \"1234567890\",");
		builder.append("  \"firstname\" : \"kkk\",");
		builder.append("  \"surname\" : \"abc\",");
		builder.append("  \"registeredPostAddress\" : {");
		builder.append("    \"street\" : \"Hasselager Centervej 17\",");
		builder.append("    \"localname\" : \"Hasselager\",");
		builder.append("    \"postalCode\" : \"8260\",");
		builder.append("    \"city\" : \"Viby J\",");
		builder.append("    \"country\" : \"Danmark\",");
		builder.append("    \"addressProtected\" : false,");
		builder.append("    \"master\" : \"TEST\",");
		builder.append("    \"masterId\" : \"TEST\",");
		builder.append("    \"prime\": true");
		builder.append("  },");
		builder.append("  \"affiliations\" : [{");
		builder.append("    \"orgUnit\": \"" + orgUnitHref + "\",");
		builder.append("    \"employeeId\": \"401\",");
		builder.append("    \"master\" : \"TEST\",");
		builder.append("    \"masterId\" : \"401\",");
		builder.append("    \"affiliationType\": \"EMPLOYEE\",");
		builder.append("    \"positionId\": \"123\",");
		builder.append("    \"positionName\": \"Ansat\",");
		builder.append("    \"localExtensions\" : { \"key1\": \"value\" }");
		builder.append("  }]");
		builder.append("}");

		this.mockMvc.perform(post("/api/persons")
				.header("ApiKey", dataGenerator.getApiKey())
				.content(builder.toString()))
				.andExpect(status().isCreated());

		this.mockMvc.perform(get("/api/persons/" + uuid)
				.header("ApiKey", dataGenerator.getApiKey()))
				.andExpect(jsonPath("$.uuid", is(uuid)))
				.andExpect(jsonPath("$.affiliations", hasSize(1)))
				.andExpect(jsonPath("$.affiliations[?(@.positionName=='Ansat')].deleted", is(Collections.singletonList(false))))
				.andExpect(jsonPath("$.affiliations[?(@.positionName=='Ansat')]._links.orgUnit.href", is(orgUnitHrefExpected)))
				.andExpect(jsonPath("$.affiliations[?(@.positionName=='Ansat')].employeeId", is(Collections.singletonList("401"))))
				.andExpect(jsonPath("$.affiliations[?(@.positionName=='Ansat')].affiliationType", is(Collections.singletonList("EMPLOYEE"))));
	}

	@Test
	public void createAndVerifyUserWithTwoEmplyoment() throws Exception{
		String uuid = UUID.randomUUID().toString();

		ResultActions resultActions = this.mockMvc.perform(get("/api/orgUnits/" + dataGenerator.getOuHRUuid())
								.header("ApiKey", dataGenerator.getApiKey()));

		JSONObject jsonObj = new JSONObject(resultActions.andReturn().getResponse().getContentAsString());

		JSONObject linksObj = (JSONObject) jsonObj.get("_links");
		JSONObject selfObj = (JSONObject) linksObj.get("self");

		String orgUnitHref = selfObj.get("href").toString();

		/* Workaround for when the projections are added to the project */
		JSONArray orgUnitHrefExpected = new JSONArray();
		orgUnitHrefExpected.add(orgUnitHref + "{?projection}");

		StringBuilder builder = new StringBuilder();
		builder.append("{");
		builder.append("  \"uuid\" : \"" + uuid + "\",");
		builder.append("  \"master\" : \"TEST\",");
		builder.append("  \"masterId\" : \"" + uuid + "\",");
		builder.append("  \"user\" : {");
		builder.append("    \"master\" : \"TEST\",");
		builder.append("    \"masterId\" : \"user234\",");
		builder.append("    \"userId\" : \"user234\"");
		builder.append("  },");
		builder.append("  \"cpr\" : \"1234567890\",");
		builder.append("  \"firstname\" : \"kkk\",");
		builder.append("  \"surname\" : \"abc\",");
		builder.append("  \"registeredPostAddress\" : {");
		builder.append("    \"street\" : \"Hasselager Centervej 17\",");
		builder.append("    \"localname\" : \"Hasselager\",");
		builder.append("    \"postalCode\" : \"8260\",");
		builder.append("    \"city\" : \"Viby J\",");
		builder.append("    \"country\" : \"Danmark\",");
		builder.append("    \"addressProtected\" : false,");
		builder.append("    \"master\" : \"TEST\",");
		builder.append("    \"masterId\" : \"TEST\",");
		builder.append("    \"prime\": true");
		builder.append("  },");
		builder.append("  \"affiliations\" : [{");
		builder.append("    \"uuid\" : \""+UUID.randomUUID().toString()+"\",");
		builder.append("    \"orgUnit\": \"" + orgUnitHref + "\",");
		builder.append("    \"employeeId\": \"401\",");
		builder.append("    \"master\" : \"TEST\",");
		builder.append("    \"masterId\" : \"401\",");
		builder.append("    \"affiliationType\": \"EMPLOYEE\",");
		builder.append("    \"positionId\": \"123\",");
		builder.append("    \"positionName\": \"Ansat\",");
		builder.append("    \"localExtensions\" : { \"key1\": \"value\" }");
		builder.append("  },{");
		builder.append("    \"uuid\" : \""+UUID.randomUUID().toString()+"\",");
		builder.append("    \"orgUnit\": \"" + orgUnitHref + "\",");
		builder.append("    \"employeeId\": \"402\",");
		builder.append("    \"master\" : \"TEST\",");
		builder.append("    \"masterId\" : \"402\",");
		builder.append("    \"affiliationType\": \"EXTERNAL\",");
		builder.append("    \"positionId\": \"124\",");
		builder.append("    \"positionName\": \"Konsulent\",");
		builder.append("    \"localExtensions\" : { \"key2\": \"value\" }");
		builder.append("  }]");
		builder.append("}");

		this.mockMvc.perform(post("/api/persons")
				.header("ApiKey", dataGenerator.getApiKey())
				.content(builder.toString()))
				.andExpect(status().isCreated());

		this.mockMvc.perform(get("/api/persons/" + uuid)
				.header("ApiKey", dataGenerator.getApiKey()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.uuid", is(uuid)))
				.andExpect(jsonPath("$.affiliations", hasSize(2)))
				.andExpect(jsonPath("$.affiliations[?(@.positionName=='Ansat')].deleted", is(Collections.singletonList(false))))
				.andExpect(jsonPath("$.affiliations[?(@.positionName=='Ansat')]._links.orgUnit.href", is(orgUnitHrefExpected)))
				.andExpect(jsonPath("$.affiliations[?(@.positionName=='Ansat')].employeeId", is(Collections.singletonList("401"))))
				.andExpect(jsonPath("$.affiliations[?(@.positionName=='Ansat')].affiliationType", is(Collections.singletonList("EMPLOYEE"))))
				.andExpect(jsonPath("$.affiliations[?(@.positionName=='Konsulent')].deleted", is(Collections.singletonList(false))))
				.andExpect(jsonPath("$.affiliations[?(@.positionName=='Konsulent')]._links.orgUnit.href", is(orgUnitHrefExpected)))
				.andExpect(jsonPath("$.affiliations[?(@.positionName=='Konsulent')].employeeId", is(Collections.singletonList("402"))))
				.andExpect(jsonPath("$.affiliations[?(@.positionName=='Konsulent')].affiliationType", is(Collections.singletonList("EXTERNAL"))));
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
