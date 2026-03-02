package dk.digitalidentity.sofd.api.orgUnitTests;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

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
public class NewOrgUnitTests {
	private MockMvc mockMvc;

	@Autowired
	private DataGenerator dataGenerator;

	@Autowired
	private WebApplicationContext context;

	@Autowired
	@Qualifier("ApiSecurityFilter")
	private FilterRegistrationBean<ApiSecurityFilter> filterRegistrationBean;

	@Test
	public void createOrgUnitAndTestExists() throws Exception {
		String uuid = UUID.randomUUID().toString();

		StringBuilder builder = new StringBuilder();
		builder.append("{");
		builder.append("  \"uuid\" : \"" + uuid + "\",");
		builder.append("  \"shortname\" : \"test\",");
		builder.append("  \"name\" : \"tester\",");
		builder.append("  \"master\" : \"TEST\",");
		builder.append("  \"masterId\" : \"" + uuid + "\",");
		builder.append("  \"cvr\" : 12345678,");
		builder.append("  \"ean\" : 1234567890,");
		builder.append("  \"senr\" : 987654321,");
		builder.append("  \"pnr\" : 1122334455,");
		builder.append("  \"costBearer\" : \"xyz\",");
		builder.append("  \"orgType\" : \"orgTypeString\",");
		builder.append("  \"orgTypeId\" : 542,");
		builder.append("  \"postAddresses\" : [{");
		builder.append("    \"street\" : \"Hasselager Centervej 17\",");
		builder.append("    \"localname\" : \"Hasselager\",");
		builder.append("    \"postalCode\" : \"8260\",");
		builder.append("    \"city\" : \"Viby J\",");
		builder.append("    \"country\" : \"Danmark\",");
		builder.append("    \"addressProtected\" : false,");
		builder.append("    \"master\" : \"TEST\",");
		builder.append("    \"masterId\" : \"TEST\",");
		builder.append("    \"prime\": true");
		builder.append("  }],");
		builder.append("  \"phones\" : [{");
		builder.append("    \"phoneNumber\" : \"22446688\",");
		builder.append("    \"prime\": true,");
		builder.append("    \"master\" : \"TEST\",");
		builder.append("    \"masterId\" : \"TEST\",");
		builder.append("    \"phoneType\" : \"MOBILE\"");
		builder.append("  }],");
		builder.append("  \"emails\" : [{");
		builder.append("    \"email\" : \"hello@mail.dk\",");
		builder.append("    \"master\" : \"TEST\",");
		builder.append("    \"masterId\" : \"TEST\",");
		builder.append("    \"prime\": true");
		builder.append("  }],");
		builder.append("  \"klePrimary\" : [");
		builder.append("    \"00.00.00\",");
		builder.append("    \"01.01.01\"");
		builder.append("  ],");
		builder.append("  \"kleSecondary\" : [");
		builder.append("    \"02.02.02\"");
		builder.append("  ]");
		builder.append("}");

		this.mockMvc.perform(post("/api/orgUnits")
				.header("ApiKey", dataGenerator.getApiKey())
				.content(builder.toString()))
				.andExpect(status().isCreated());

		this.mockMvc.perform(get("/api/orgUnits")
				.header("ApiKey", dataGenerator.getApiKey()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.orgUnits", hasSize(6)));

		this.mockMvc.perform(get("/api/orgUnits/" + uuid)
				.header("ApiKey", dataGenerator.getApiKey()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.uuid", is(uuid)))
				.andExpect(jsonPath("$.deleted", is(false)))
				.andExpect(jsonPath("$.costBearer", is("xyz")))
				.andExpect(jsonPath("$.orgType", is("orgTypeString")))
				.andExpect(jsonPath("$.orgTypeId", is(542)))
				.andExpect(jsonPath("$.shortname", is("test")))
				.andExpect(jsonPath("$.cvr", is(12345678)))
				.andExpect(jsonPath("$.ean", is(1234567890)))
				.andExpect(jsonPath("$.senr", is(987654321)))
				.andExpect(jsonPath("$.pnr", is(1122334455)))
				.andExpect(jsonPath("$.name", is("tester")))
				.andExpect(jsonPath("$.phones", hasSize(1)))
				.andExpect(jsonPath("$.postAddresses", hasSize(1)))
				.andExpect(jsonPath("$.emails", hasSize(1)))
				.andExpect(jsonPath("$.kleSecondary", hasSize(1)))
				.andExpect(jsonPath("$.klePrimary", hasSize(2)));
	}

	@Test
	public void createOrgUnitWithMandatoryFieldsOnly() throws Exception{
		String uuid = UUID.randomUUID().toString();

		StringBuilder builder = new StringBuilder();
		builder.append("{");
		builder.append("  \"uuid\" : \"" + uuid + "\",");
		builder.append("  \"name\" : \"tester\",");
		builder.append("  \"master\" : \"TEST\",");
		builder.append("  \"masterId\" : \"" + uuid + "\",");
		builder.append("  \"shortname\" : \"TESTER\"");
		builder.append("}");

		this.mockMvc.perform(post("/api/orgUnits")
				.header("ApiKey", dataGenerator.getApiKey())
				.content(builder.toString()))
				.andExpect(status().isCreated());

		this.mockMvc.perform(get("/api/orgUnits/" + uuid)
				.header("ApiKey", dataGenerator.getApiKey()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.uuid", is(uuid)))
				.andExpect(jsonPath("$.name", is("tester")))
				.andExpect(jsonPath("$.shortname", is("TESTER")));
	}

	@Test
	public void createOrgUnitWithInvalidInputAndVerifyCallFailing() throws Exception{
		String uuid = UUID.randomUUID().toString();

		StringBuilder builder = new StringBuilder();
		builder.append("{");
		builder.append("  \"uuid\" : \"" + uuid + "\",");
		builder.append("  \"losOrgId\" : 21");
		builder.append("}");

		this.mockMvc.perform(post("/api/orgUnits")
				.header("ApiKey", dataGenerator.getApiKey())
				.content(builder.toString()))
				.andExpect(status().isBadRequest());
	}

	@Test
	public void createOrgUnitWithInvalidJsonAndVerifyCallFailing() throws Exception{
		String uuid = UUID.randomUUID().toString();

		StringBuilder builder = new StringBuilder();
		builder.append("{");
		builder.append("  \"uuid\" : \"" + uuid + "\",");
		builder.append("  \"name\" : \"tester\",");
		builder.append("  \"losOrgId\" : 21,");
		builder.append("  \"phoneNumber\" : [{");
		builder.append("    \"phoneNumber\" : \"22446688\",");
		builder.append("    \"mobile\" : true");
		builder.append("  },");
		builder.append("  \"phoneNumber\" : {");
		builder.append("    \"phoneNumber\" : \"11335577\",");
		builder.append("    \"mobile\" : false");
		builder.append("  }]");
		builder.append("}");

		this.mockMvc.perform(post("/api/orgUnits")
				.header("ApiKey", dataGenerator.getApiKey())
				.content(builder.toString()))
				.andExpect(status().isBadRequest());
	}

	@Test
	public void createAndVerifyLocalExtensionsOnOrgUnit() throws Exception{
		String uuid = UUID.randomUUID().toString();

		StringBuilder builder = new StringBuilder();
		builder.append("{");
		builder.append("  \"uuid\" : \"" + uuid + "\",");
		builder.append("  \"name\" : \"tester\",");
		builder.append("  \"master\" : \"TEST\",");
		builder.append("  \"masterId\" : \"" + uuid + "\",");
		builder.append("  \"shortname\" : \"TESTER\",");
		builder.append("  \"localExtensions\" : {");
		builder.append("    \"keyCardId\" : 73184,");
		builder.append("    \"shoeSize\" : \"43 EU\",");
		builder.append("    \"lederOplysninger\" : {");
		builder.append("    	\"afdeling\" : \"IT og digitalisering\",");
		builder.append("    	\"medarbejederansvar\" : false");
		builder.append("  	}");
		builder.append("  }");
		builder.append("}");

		this.mockMvc.perform(post("/api/orgUnits")
				.header("ApiKey", dataGenerator.getApiKey())
				.content(builder.toString()))
				.andExpect(status().isCreated());

		this.mockMvc.perform(get("/api/orgUnits/" + uuid)
				.header("ApiKey", dataGenerator.getApiKey()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.uuid", is(uuid)))
				.andExpect(jsonPath("$.localExtensions.keyCardId", is(73184)))
				.andExpect(jsonPath("$.localExtensions.shoeSize", is("43 EU")))
				.andExpect(jsonPath("$.localExtensions.lederOplysninger.afdeling", is("IT og digitalisering")))
				.andExpect(jsonPath("$.localExtensions.lederOplysninger.medarbejederansvar", is(false)));
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
