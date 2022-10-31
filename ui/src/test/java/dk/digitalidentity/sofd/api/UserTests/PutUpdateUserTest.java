package dk.digitalidentity.sofd.api.UserTests;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Iterator;
import java.util.UUID;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import dk.digitalidentity.sofd.DataGenerator;
import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.security.ApiSecurityFilter;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestPropertySource(locations="classpath:test.properties")
@ActiveProfiles({ "test" })
public class PutUpdateUserTest {
	private MockMvc mockMvc;

	@Autowired
	private DataGenerator dataGenerator;

	@Autowired
	private WebApplicationContext context;

	@Autowired
	@Qualifier("ApiSecurityFilter")
	private FilterRegistrationBean<ApiSecurityFilter> filterRegistrationBean;

	@Test
	public void validPutUpdateOnUser() throws Exception{
		ResultActions resultActions = this.mockMvc.perform(get("/api/persons/" + dataGenerator.getUser8Uuid()).header("ApiKey", dataGenerator.getApiKey()));

		String jsonString = resultActions.andReturn().getResponse().getContentAsString();
		JSONObject jsonObj = new JSONObject(jsonString);

		//remove value
		jsonObj.remove("phones");

		//change value
		jsonObj.remove("cpr");
		jsonObj.put("cpr", "0101018099");

		//set value
		jsonObj.put("chosenName", "New Chosenname");

		this.mockMvc.perform(put("/api/persons/" + dataGenerator.getUser8Uuid())
				.header("ApiKey", dataGenerator.getApiKey())
				.content(jsonObj.toString()))
				.andExpect(status().isNoContent());

		this.mockMvc.perform(get("/api/persons/" + dataGenerator.getUser8Uuid())
				.header("ApiKey", dataGenerator.getApiKey()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.phones", hasSize(0)))
				.andExpect(jsonPath("$.cpr", is("0101018099")))
				.andExpect(jsonPath("$.chosenName", is("New Chosenname")));
	}

	@Test
	public void validPutUpdateOnNonExistingUser() throws Exception{
		ResultActions resultActions = this.mockMvc.perform(get("/api/persons/" + dataGenerator.getUser1Uuid()).header("ApiKey", dataGenerator.getApiKey()));

		this.mockMvc.perform(put("/api/persons/" + UUID.randomUUID().toString())
				.header("ApiKey", dataGenerator.getApiKey())
				.content(resultActions.andReturn().getResponse().getContentAsString()))
				.andExpect(status().isBadRequest());
	}

	@Test
	public void invalidPutUpdateOnUser() throws Exception{
		ResultActions resultActions = this.mockMvc.perform(get("/api/persons/" + dataGenerator.getUser1Uuid()).header("ApiKey", dataGenerator.getApiKey()));

		JSONObject jsonObj = new JSONObject(resultActions.andReturn().getResponse().getContentAsString());
		
		jsonObj.remove("registeredPostAddress");
		jsonObj.put("registeredPostAddress", "");

		this.mockMvc.perform(put("/api/persons/" + dataGenerator.getUser1Uuid())
				.header("ApiKey", dataGenerator.getApiKey())
				.content(jsonObj.toString()))
				.andExpect(status().isBadRequest());
	}

	@Test
	public void putUpdateLocalExtensionsOnUser() throws Exception{
		RestTemplate template = new RestTemplate();

		HttpHeaders headers = new HttpHeaders();
		headers.set("ApiKey", dataGenerator.getApiKey());

		HttpEntity<?> httpEntity = new HttpEntity<Object>(headers);

		ResponseEntity<Person> userResponse = template.exchange("https://localhost:9020/api/persons/" + dataGenerator.getUser1Uuid(), HttpMethod.GET, httpEntity, Person.class);
		Person user = userResponse.getBody();

		StringBuilder builder = new StringBuilder();
		builder.append("  {");
		builder.append("    \"keyCardId\" : 73184,");
		builder.append("    \"shoeSize\" : \"43 EU\",");
		builder.append("    \"lederOplysninger\" : {");
		builder.append("    	\"afdeling\" : \"IT og digitalisering\",");
		builder.append("    	\"medarbejederansvar\" : false");
		builder.append("  	}");
		builder.append("  }");

		user.setLocalExtensions(builder.toString());

		ObjectMapper mapper = new ObjectMapper();
		String payload = mapper.writeValueAsString(user);

		this.mockMvc.perform(put("/api/persons/" + dataGenerator.getUser1Uuid())
				.header("ApiKey", dataGenerator.getApiKey())
				.content(payload))
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
	public void putUpdateEmploymentOnUserRemove() throws Exception{
		RestTemplate template = new RestTemplate();

		HttpHeaders headers = new HttpHeaders();
		headers.set("ApiKey", dataGenerator.getApiKey());

		HttpEntity<?> httpEntity = new HttpEntity<Object>(headers);

		ResponseEntity<Person> userResponse = template.exchange("https://localhost:9020/api/persons/" + dataGenerator.getUser10Uuid(), HttpMethod.GET, httpEntity, Person.class);
		Person user = userResponse.getBody();

		Iterator<Affiliation> iterator = user.getAffiliations().iterator();
		iterator.next();
		iterator.remove();

		ObjectMapper mapper = new ObjectMapper();
		String payload = mapper.writeValueAsString(user);

		this.mockMvc.perform(put("/api/persons/" + dataGenerator.getUser10Uuid())
				.header("ApiKey", dataGenerator.getApiKey())
				.content(payload))
				.andExpect(status().isNoContent());

		this.mockMvc.perform(get("/api/persons/" + dataGenerator.getUser10Uuid())
				.header("ApiKey", dataGenerator.getApiKey()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.affiliations", hasSize(1)));
	}

	@Test
	public void putUpdateEmploymentOnUserGive() throws Exception{
        ResultActions resultActionsOrgUnit = this.mockMvc.perform(get("/api/orgUnits/" + dataGenerator.getOuSundhedUuid()).header("ApiKey", dataGenerator.getApiKey()));

        JSONObject jsonObjOrgUnit = new JSONObject(resultActionsOrgUnit.andReturn().getResponse().getContentAsString());
        JSONObject linksObj = (JSONObject) jsonObjOrgUnit.get("_links");
        JSONObject selfObj = (JSONObject) linksObj.get("self");

        String orgUnitHref = selfObj.get("href").toString();

        String userUuid = UUID.randomUUID().toString();

		StringBuilder builder = new StringBuilder();
		builder.append("{");
		builder.append("  \"uuid\" : \"" + userUuid + "\",");
		builder.append("  \"cpr\" : \"1234567890\",");
		builder.append("  \"master\" : \"TEST\",");
		builder.append("  \"masterId\" : \"" + userUuid + "\",");
		builder.append("  \"firstname\" : \"kkk\",");
		builder.append("  \"surname\" : \"abc\",");
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

		this.mockMvc.perform(get("/api/persons/" + userUuid)
				.header("ApiKey", dataGenerator.getApiKey()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.affiliations", hasSize(0)));

		builder = new StringBuilder();
		builder.append("{");
		builder.append("  \"uuid\" : \"" + userUuid + "\",");
		builder.append("  \"cpr\" : \"1234567890\",");
		builder.append("  \"firstname\" : \"kkk\",");
		builder.append("  \"master\" : \"TEST\",");
		builder.append("  \"masterId\" : \"" + userUuid + "\",");
		builder.append("  \"surname\" : \"abc\",");
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
		builder.append("  },");
		builder.append("  \"affiliations\" : [{");
		builder.append("    \"orgUnit\": \"" + orgUnitHref + "\",");
		builder.append("    \"employeeId\": \"401\",");
		builder.append("    \"master\" : \"TEST\",");
		builder.append("    \"masterId\" : \"401\",");
		builder.append("    \"affiliationType\": \"EMPLOYEE\",");
		builder.append("    \"positionId\": \"123\",");
		builder.append("    \"positionName\": \"Ansat\"");
		builder.append("  }]");
		builder.append("}");

		this.mockMvc.perform(put("/api/persons/" + userUuid)
				.header("ApiKey", dataGenerator.getApiKey())
				.content(builder.toString()))
				.andExpect(status().isNoContent());

		this.mockMvc.perform(get("/api/persons/" + userUuid)
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
