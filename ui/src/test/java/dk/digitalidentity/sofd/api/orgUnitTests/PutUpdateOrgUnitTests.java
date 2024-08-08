package dk.digitalidentity.sofd.api.orgUnitTests;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
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
import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.security.ApiSecurityFilter;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestPropertySource(locations="classpath:test.properties")
@ActiveProfiles({ "test" })
public class PutUpdateOrgUnitTests {
	private MockMvc mockMvc;

	@Autowired
	private DataGenerator dataGenerator;

	@Autowired
	private WebApplicationContext context;

	@Autowired
	@Qualifier("ApiSecurityFilter")
	private FilterRegistrationBean<ApiSecurityFilter> filterRegistrationBean;

	@Test
	public void validPutUpdateOnOrgUnit() throws Exception{
		RestTemplate template = new RestTemplate();

		HttpHeaders headers = new HttpHeaders();
		headers.set("ApiKey", dataGenerator.getApiKey());

		HttpEntity<?> httpEntity = new HttpEntity<Object>(headers);

		ResponseEntity<OrgUnit> orgUnitResponse = template.exchange("https://localhost:9020/api/orgUnits/" + dataGenerator.getOuHRUuid(), HttpMethod.GET, httpEntity, OrgUnit.class);
		OrgUnit orgUnit = orgUnitResponse.getBody();

		orgUnit.setSourceName("HR test");
		orgUnit.setPnr(null);

		ObjectMapper mapper = new ObjectMapper();
		String payload = mapper.writeValueAsString(orgUnit);

		this.mockMvc.perform(put("/api/orgUnits/" + dataGenerator.getOuHRUuid())
				.header("ApiKey", dataGenerator.getApiKey())
				.content(payload))
				.andExpect(status().isNoContent());

		this.mockMvc.perform(get("/api/orgUnits/" + dataGenerator.getOuHRUuid())
				.header("ApiKey", dataGenerator.getApiKey()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.pnr", is(IsNull.nullValue())))
				.andExpect(jsonPath("$.name", is("HR test")));
	}

	@Test
	public void validPutUpdateOnNonExistingOrgUnit() throws Exception{
		RestTemplate template = new RestTemplate();

		HttpHeaders headers = new HttpHeaders();
		headers.set("ApiKey", dataGenerator.getApiKey());

		HttpEntity<?> httpEntity = new HttpEntity<Object>(headers);

		ResponseEntity<OrgUnit> orgUnitResponse = template.exchange("https://localhost:9020/api/orgUnits/" + dataGenerator.getOuHRUuid(), HttpMethod.GET, httpEntity, OrgUnit.class);
		OrgUnit orgUnit = orgUnitResponse.getBody();

		orgUnit.setSourceName("HR test");
		orgUnit.setPnr(null);

		ObjectMapper mapper = new ObjectMapper();
		String payload = mapper.writeValueAsString(orgUnit);

		this.mockMvc.perform(put("/api/orgUnits/" + UUID.randomUUID().toString())
				.header("ApiKey", dataGenerator.getApiKey())
				.content(payload))
				.andExpect(status().isBadRequest());
	}

	@Test
	public void invalidPutUpdateOnOrgUnit() throws Exception{
		RestTemplate template = new RestTemplate();

		HttpHeaders headers = new HttpHeaders();
		headers.set("ApiKey", dataGenerator.getApiKey());

		HttpEntity<?> httpEntity = new HttpEntity<Object>(headers);

		ResponseEntity<OrgUnit> orgUnitResponse = template.exchange("https://localhost:9020/api/orgUnits/" + dataGenerator.getOuHRUuid(), HttpMethod.GET, httpEntity, OrgUnit.class);
		OrgUnit orgUnit = orgUnitResponse.getBody();

		orgUnit.setSourceName(null);

		ObjectMapper mapper = new ObjectMapper();
		String payload = mapper.writeValueAsString(orgUnit);

		this.mockMvc.perform(put("/api/orgUnits/" + dataGenerator.getOuHRUuid())
				.header("ApiKey", dataGenerator.getApiKey())
				.content(payload))
				.andExpect(status().isBadRequest());
	}

	@Test
	public void dropPhonesOnOrgUnit() throws Exception{
		ResultActions resultActions = this.mockMvc.perform(get("/api/orgUnits/" + dataGenerator.getOuKommuneUuid()).header("ApiKey", dataGenerator.getApiKey()));

		String jsonString = resultActions.andReturn().getResponse().getContentAsString();
		JSONObject jsonObj = new JSONObject(jsonString);

		//remove value
		jsonObj.remove("phones");

		this.mockMvc.perform(put("/api/orgUnits/" + dataGenerator.getOuKommuneUuid())
				.header("ApiKey", dataGenerator.getApiKey())
				.content(jsonObj.toString()))
				.andExpect(status().isNoContent());

		this.mockMvc.perform(get("/api/orgUnits/" + dataGenerator.getOuKommuneUuid())
				.header("ApiKey", dataGenerator.getApiKey()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.phones", hasSize(0)));
	}
	
	@Test
	public void putUpdateLocalExtensionsOnOrgUnit() throws Exception{
		RestTemplate template = new RestTemplate();

		HttpHeaders headers = new HttpHeaders();
		headers.set("ApiKey", dataGenerator.getApiKey());

		HttpEntity<?> httpEntity = new HttpEntity<Object>(headers);

		ResponseEntity<OrgUnit> orgUnitResponse = template.exchange("https://localhost:9020/api/orgUnits/" + dataGenerator.getOuHRUuid(), HttpMethod.GET, httpEntity, OrgUnit.class);
		OrgUnit orgUnit = orgUnitResponse.getBody();

		StringBuilder builder = new StringBuilder();
		builder.append("  {");
		builder.append("    \"keyCardId\" : 73184,");
		builder.append("    \"shoeSize\" : \"43 EU\",");
		builder.append("    \"lederOplysninger\" : {");
		builder.append("    	\"afdeling\" : \"IT og digitalisering\",");
		builder.append("    	\"medarbejederansvar\" : false");
		builder.append("  	}");
		builder.append("  }");

		orgUnit.setLocalExtensions(builder.toString());

		ObjectMapper mapper = new ObjectMapper();
		String payload = mapper.writeValueAsString(orgUnit);

		this.mockMvc.perform(put("/api/orgUnits/" + dataGenerator.getOuHRUuid())
				.header("ApiKey", dataGenerator.getApiKey())
				.content(payload))
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
	public void putUpdateEngangementOnOrgUnitRemove() throws Exception{
		RestTemplate template = new RestTemplate();

		HttpHeaders headers = new HttpHeaders();
		headers.set("ApiKey", dataGenerator.getApiKey());

		HttpEntity<?> httpEntity = new HttpEntity<Object>(headers);

		ResponseEntity<OrgUnit> orgUnitResponse = template.exchange("https://localhost:9020/api/orgUnits/" + dataGenerator.getOuHRUuid(), HttpMethod.GET, httpEntity, OrgUnit.class);
		OrgUnit orgUnit = orgUnitResponse.getBody();

		orgUnit.setAffiliations(new ArrayList<>());

		ObjectMapper mapper = new ObjectMapper();
		String payload = mapper.writeValueAsString(orgUnit);

		this.mockMvc.perform(put("/api/orgUnits/" + dataGenerator.getOuHRUuid())
				.header("ApiKey", dataGenerator.getApiKey())
				.content(payload))
				.andExpect(status().isNoContent());

		this.mockMvc.perform(get("/api/orgUnits/" + dataGenerator.getOuHRUuid())
				.header("ApiKey", dataGenerator.getApiKey()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.uuid", is(dataGenerator.getOuHRUuid())))
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
