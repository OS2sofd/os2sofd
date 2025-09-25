package dk.digitalidentity.sofd.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dk.digitalidentity.sofd.config.RoleConstants;
import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.security.SecurityUtil;

@Service
public class SearchService {

	@Autowired
	private PersonService personService;

	@Autowired
	private OrgUnitService orgUnitService;

	private static final int MAX_RESULTS = 20;

	public List<Map<String, String>> search(String query) {
		List<Map<String, String>> results = new ArrayList<>();

		// For consistency's sake
		query = query.toLowerCase();

		// Organisation
		if ("organisationsenheder".startsWith(query) || "enheder".startsWith(query)) {
			Map<String, String> result = new HashMap<>();
			result.put("name", "Enheder (Enhed)");
			result.put("url", "/ui/orgunit");
			results.add(result);
		}

		if ("skoleelever".startsWith(query)) {
			Map<String, String> result = new HashMap<>();
			result.put("name", "Skoleelever (Hat)");
			result.put("url", "/ui/student");
			results.add(result);
		}

		if ("personer".startsWith(query)) {
			Map<String, String> result = new HashMap<>();
			result.put("name", "Personer (Person)");
			result.put("url", "/ui/person");
			results.add(result);
		}

		if ("rapporter".startsWith(query)) {
			Map<String, String> result = new HashMap<>();
			result.put("name", "Rapporter (Book)");
			result.put("url", "/ui/report/reports");
			results.add(result);
		}

		if ("ledere".startsWith(query)) {
			Map<String, String> result = new HashMap<>();
			result.put("name", "Ledere (Enhed)");
			result.put("url", "/ui/report/managers");
			results.add(result);
		}

		if ("historiske ad konti".startsWith(query) || "ad".startsWith(query) || "konti".startsWith(query)) {
			Map<String, String> result = new HashMap<>();
			result.put("name", "Historiske ad konti (Enhed)");
			result.put("url", "/ui/report/users");
			results.add(result);
		}

		if ("email kø".startsWith(query) || "kø".startsWith(query)) {
			Map<String, String> result = new HashMap<>();
			result.put("name", "Email kø (Mail)");
			result.put("url", "/ui/report/emailqueue");
			results.add(result);
		}

		if (SecurityUtil.hasRole(RoleConstants.USER_ROLE_LOS_ADMIN)) {
			if ("fremtidige ændringer".startsWith(query) || "ændringer".startsWith(query)) {
				Map<String, String> result = new HashMap<>();
				result.put("name", "Fremtidige ændringer (Forward)");
				result.put("url", "/ui/changes/orgunit");
				results.add(result);
			}

			if ("henvendelsessteder".startsWith(query)) {
				Map<String, String> result = new HashMap<>();
				result.put("name", "Henvendelsessteder (Marker)");
				result.put("url", "/ui/contactplaces");
				results.add(result);
			}
		}


		if ("auditlogs".startsWith(query) || "log".startsWith(query)) {
			Map<String, String> result = new HashMap<>();
			result.put("name", "Auditlog (Table)");
			result.put("url", "/ui/auditlogs");
			results.add(result);
		}

		// Brugermanual
		if ("manual".startsWith(query) || "brugermanual".startsWith(query)) {
			Map<String, String> result = new HashMap<>();
			result.put("name", "Brugermanual (Book)");
			result.put("url", "/download/brugermanual.pdf");
			results.add(result);
		}

		// Kommunikation
		if (SecurityUtil.hasRole(RoleConstants.USER_ROLE_SMS)) {
			if ("send sms-besked".startsWith(query) || "send".startsWith(query) || "sms-besked".startsWith(query) || "opret besked".startsWith(query)) {
				Map<String, String> result = new HashMap<>();
				result.put("name", "Opret SMS-besked (Paper)");
				result.put("url", "/ui/sms/message");
				results.add(result);
			}

			if ("sms".startsWith(query) || "skabeloner".startsWith(query)) {
				Map<String, String> result = new HashMap<>();
				result.put("name", "SMS skabeloner (List)");
				result.put("url", "/ui/template/list");
				results.add(result);
			}

			if ("sms".startsWith(query) || "log".startsWith(query)) {
				Map<String, String> result = new HashMap<>();
				result.put("name", "SMS log (Book)");
				result.put("url", "/ui/sms/log");
				results.add(result);
			}

			if ("sms".startsWith(query) || "konfiguration".startsWith(query) || "kodeord".startsWith(query) || "påmindelser".startsWith(query)) {
				Map<String, String> result = new HashMap<>();
				result.put("name", "Kodeordsskifte (Bell)");
				result.put("url", "/ui/sms/pwdreminders");
				results.add(result);
			}

			if ("sms".startsWith(query) || "konfiguration".startsWith(query) || "låst".startsWith(query) || "påmindelser".startsWith(query) || "kodeord".startsWith(query) || "spærret".startsWith(query)) {
				Map<String, String> result = new HashMap<>();
				result.put("name", "Kodeord spærret (Bell)");
				result.put("url", "/ui/sms/pwdlocked");
				results.add(result);
			}
		}

		if (SecurityUtil.hasRole(RoleConstants.USER_ROLE_EDIT)) {
			if ("adviser".startsWith(query)) {
				Map<String, String> result = new HashMap<>();
				result.put("name", "Adviser (Tasks)");
				result.put("url", "/ui/report/notifications");
				results.add(result);
			}
		}

		if (SecurityUtil.hasRole(RoleConstants.USER_ROLE_EDIT) || SecurityUtil.hasRole(RoleConstants.DATA_ROLE_MANAGER)) {
			if ("brugerkonto bestillinger".startsWith(query) || "bestillinger".startsWith(query) || "afventer".startsWith(query)) {
				Map<String, String> result = new HashMap<>();
				result.put("name", "Afventer godkendelse (Table)");
				result.put("url", "/ui/account/order/approve");
				results.add(result);
			}

			if ("godkendelser på brugeroprettelser".startsWith(query) || "brugeroprettelser".startsWith(query) || "godkendelseslog".startsWith(query)) {
				Map<String, String> result = new HashMap<>();
				result.put("name", "Godkendelseslog (Status)");
				result.put("url", "/ui/report/accountorderapprovals");
				results.add(result);
			}

			if ("brugerkonto bestillinger".startsWith(query) || "bestillinger".startsWith(query) || "ordre status".startsWith(query)) {
				Map<String, String> result = new HashMap<>();
				result.put("name", "Ordre Status (Status)");
				result.put("url", "/ui/report/accountorders");
				results.add(result);
			}
		}

		// Administrator
		if (SecurityUtil.hasRole(RoleConstants.USER_ROLE_ADMIN)) {
			if ("se klienter".startsWith(query) || "klienter".startsWith(query) || "rediger".startsWith(query)) {
				Map<String, String> result = new HashMap<>();
				result.put("name", "List klienter (Multiple)");
				result.put("url", "/ui/client/list");
				results.add(result);
			}

			if ("opret ny klient".startsWith(query) || "klient".startsWith(query) || "ny".startsWith(query)) {
				Map<String, String> result = new HashMap<>();
				result.put("name", "Opret ny klient (Multiple)");
				result.put("url", "/ui/client/new");
				results.add(result);
			}

			if ("mailskabeloner".startsWith(query) || "administration".startsWith(query) || "ny".startsWith(query) || "rediger".startsWith(query)) {
				Map<String, String> result = new HashMap<>();
				result.put("name", "Administration af mailskabeloner (Mail)");
				result.put("url", "/ui/admin/mailtemplates");
				results.add(result);
			}

			if ("sletning".startsWith(query) || "personer".startsWith(query) || "fysisk sletning".startsWith(query) || "inaktiv".startsWith(query)) {
				Map<String, String> result = new HashMap<>();
				result.put("name", "Fysisk sletning af inaktive personer (Clock)");
				result.put("url", "/ui/admin/cleanup/persondeleteperiod");
				results.add(result);
			}

			if ("opus".startsWith(query) || "indlæsningsfiltre".startsWith(query) || "indstillinger".startsWith(query) || "filtre".startsWith(query)) {
				Map<String, String> result = new HashMap<>();
				result.put("name", "Indlæsningsfiltre (Cog)");
				result.put("url", "/ui/admin/opusfilter");
				results.add(result);
			}

			if ("opus".startsWith(query) || "ekstra tilhørsforhold".startsWith(query) || "indstillinger".startsWith(query) || "tilhørsforhold".startsWith(query)) {
				Map<String, String> result = new HashMap<>();
				result.put("name", "Ekstra tilhørsforhold (Cog)");
				result.put("url", "/ui/admin/opusautoaff");
				results.add(result);
			}

			if ("organisationsdiagrammer".startsWith(query) || "diagrammer".startsWith(query)) {
				Map<String, String> result = new HashMap<>();
				result.put("name", "Organisationsdiagrammer (Sitemap)");
				result.put("url", "/ui/admin/chart/list");
				results.add(result);
			}

			if ("indstil advis dannelse".startsWith(query) || "dannelse".startsWith(query) || "advis".startsWith(query)) {
				Map<String, String> result = new HashMap<>();
				result.put("name", "Indstil advis dannelse (Exclamation)");
				result.put("url", "/ui/admin/notifications/settings");
				results.add(result);
			}

			if ("funktioner".startsWith(query)) {
				Map<String, String> result = new HashMap<>();
				result.put("name", "Funktioner (Sitemap)");
				result.put("url", "/ui/admin/functionhierarchy/functions");
				results.add(result);
			}

			if ("funktionsfacetter".startsWith(query) || "facetter".startsWith(query)) {
				Map<String, String> result = new HashMap<>();
				result.put("name", "Funktionsfacetter (Puzzle)");
				result.put("url", "/ui/admin/functionhierarchy/facets");
				results.add(result);
			}

			if ("organisationer".startsWith(query)) {
				Map<String, String> result = new HashMap<>();
				result.put("name", "Organisationer (Sitemap)");
				result.put("url", "/ui/organisation");
				results.add(result);
			}

			if ("stedfortræderkontekst".startsWith(query) || "kontekst".startsWith(query)) {
				Map<String, String> result = new HashMap<>();
				result.put("name", "Stedfortræderkontekster (Enhed)");
				result.put("url", "/ui/substituteContext");
				results.add(result);
			}

			if ("brugerkontotyper".startsWith(query) || "typer".startsWith(query) || "konto".startsWith(query)) {
				Map<String, String> result = new HashMap<>();
				result.put("name", "Brugerkontotyper (Person)");
				result.put("url", "/ui/usertype");
				results.add(result);
			}

			if ("enhendstyper".startsWith(query) || "typer".startsWith(query)) {
				Map<String, String> result = new HashMap<>();
				result.put("name", "Enhendstyper (Enhed)");
				result.put("url", "/ui/orgunittypes");
				results.add(result);
			}

			if ("forbudte brugernavne".startsWith(query) || "brugernavne".startsWith(query)) {
				Map<String, String> result = new HashMap<>();
				result.put("name", "Forbudte brugernavne (Ban)");
				result.put("url", "/ui/badwords");
				results.add(result);
			}

			if ("tags".startsWith(query)) {
				Map<String, String> result = new HashMap<>();
				result.put("name", "Tags (Tag)");
				result.put("url", "/ui/tags");
				results.add(result);
			}

			if ("institutioner".startsWith(query)) {
				Map<String, String> result = new HashMap<>();
				result.put("name", "Institutioner (University)");
				result.put("url", "/ui/institutions");
				results.add(result);
			}

			if ("konfigurable features".startsWith(query)) {
				Map<String, String> result = new HashMap<>();
				result.put("name", "Konfigurable features (Multiple)");
				result.put("url", "/ui/admin/featuredocumentation");
				results.add(result);
			}
		}

		// Person search based on name or CPR
		personAndOuLookUp(query, results);

		return results;
	}

	private void personAndOuLookUp(String query, List<Map<String, String>> results) {
		if (query.length() > 1) {
			if( results.size() < MAX_RESULTS ) {
				List<Person> personSearchResult = personService.searchPersons(query, SecurityUtil.hasRole(RoleConstants.USER_ROLE_CPR_ACCESS));
				for (Person person : personSearchResult) {
					Map<String, String> result = new HashMap<>();
					result.put("name", person.getFirstname() + " " + person.getSurname() + " (Person)");
					result.put("url", "/ui/person/view/" + person.getUuid());
					results.add(result);
				}
			}

			if( results.size() < MAX_RESULTS ) {
				List<OrgUnit> orgUnitSearchResult = orgUnitService.searchOrgUnits(query);
				for (OrgUnit orgunit : orgUnitSearchResult) {
					Map<String, String> result = new HashMap<>();
					result.put("name", orgunit.getSourceName() +  " (Enhed)");
					result.put("url", "/ui/orgunit/view/" + orgunit.getUuid());
					results.add(result);
				}
			}
		}
	}


}
