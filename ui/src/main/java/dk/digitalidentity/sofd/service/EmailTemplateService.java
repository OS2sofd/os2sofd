package dk.digitalidentity.sofd.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.SubstituteOrgUnitAssignment;
import dk.digitalidentity.sofd.dao.model.enums.EmployeeFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import dk.digitalidentity.sofd.dao.EmailTemplateDao;
import dk.digitalidentity.sofd.dao.model.EmailTemplate;
import dk.digitalidentity.sofd.dao.model.EmailTemplateChild;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.SubstituteAssignment;
import dk.digitalidentity.sofd.dao.model.enums.EmailTemplateType;
import dk.digitalidentity.sofd.dao.model.enums.SendTo;

@Service
public class EmailTemplateService {

	@Autowired
	private EmailTemplateDao emailTemplateDao;

	@Autowired
	private OrgUnitService orgUnitService;

	public List<EmailTemplate> findAll() {
		List<EmailTemplate> result = new ArrayList<>();
		
		for (EmailTemplateType type : EmailTemplateType.values()) {
			result.add(findByTemplateType(type));
		}
		
		return result;
	}
	
	public EmailTemplateChild generateDefaultChild(EmailTemplate template) {
		String title = "Overskrift";
		String message = "Besked";
		long daysBeforeEvent = 0;
		
		switch (template.getTemplateType()) {
			case AD_CREATE_EMPLOYEE:
				title = "Brugerkonto oprettet";
				message = "Kære {modtager}\n<br/>\n<br/>\nDer er blevet oprettet en brugerkonto til dig med brugernavn\n<br/>\n<br/>\n{kontonavn}";
				break;
			case AD_CREATE_SUBSTITUTE:
				title = "Vikarkonto tildelt";
				message = "Kære {modtager}\n<br/>\n<br/>Der er blevet tildelt en vikarkonto til dig med brugernavn\n<br/>\n<br/>\n{kontonavn}";
				break;
			case AD_CREATE_FAILED:
				title = "Oprettelse af brugerkonto fejlet";
				message = "Kære {modtager}\n<br/>\n<br/>\nBestillingen af en brugerkonto til {medarbejder} er fejlet, og kontoen er ikke blevet oprettet.";
				break;
			case AD_CREATE_MANAGER:
				title = "Brugerkonto oprettet";
				message = "Kære {modtager}\n<br/>\n<br/>\nDer er blevet oprettet en brugerkonto til {medarbejder} med brugernavn\n<br/>\n<br/>\n{kontonavn}";
				break;
			case AD_DISABLE_MANAGER:
				title = "Brugerkonto lukket";
				message = "Kære {modtager}\n<br/>\n<br/>\nBrugerkontoen for {medarbejder} er blevet lukket. Der er tale om brugerkontoen med brugernavnet\n<br/>\n<br/>\n{kontonavn}";
				break;
			case EXCHANGE_CREATE_EMPLOYEE:
				title = "Mailkonto oprettet";
				message = "Kære {modtager}\n<br/>\n<br/>Der er blevet oprettet en mailkonto til dig.";
				break;
			case EXCHANGE_CREATE_EMPLOYEE2:
				title = "Mailkonto oprettet 2";
				message = "Kære {modtager}\n<br/>\n<br/>Der er blevet oprettet en mailkonto til dig.";
				break;
			case NEW_AFFILIATION:
				title = "Ny medarbejder";
				// følgende er også muligt, men ønskes ikke vist i std skabelonen
				// Uuid for tilhørsforhold: {tilhørsforholduuid}
				message = "Kære {modtager}\n<br/>\n<br/>{medarbejder} starter i din afdeling {orgenhed} d. {tidspunkt}.";
				break;
			case NEW_EMPLOYEE_WELCOME:
				title = "Velkommen, ny medarbejder";
				message = "Kære {leder}\n<br/>\n<br/>Der starter en ny medarbejder: {medarbejder} i din afdeling";
				break;
			case NEW_EMPLOYEE_REMINDER:
				title = "Påmindelse om ny medarbejder";
				message = "Kære {leder}\n<br/>\n<br/>Husk, der er startet en ny medarbejder: {medarbejder} i din afdeing";
				break;
			case ORGUNIT_CHANGES:
				title = "Organisationsændringer";
				message = "Kære {modtager}\n<br/>\n<br/>Der er sket ændringer i organisationen: {ændringer}";
				break;
			case EXCHANGE_CREATE_EMPLOYEE_EBOKS:
				title = "Exchange konto oprettet";
				message = "Kære {modtager}\n<br/>\n<br/>\nDer er blevet oprettet en mailkonto til dig med adressen {exchange} som er knyttet til AD kontoen {kontonavn}.";
				break;
			case SUBSTITUTE_STOPS:
				title = "Stedfortræder har ændret ansættelse";
				message = "Kære {modtager}\n<br/>\n<br/>Din stedfortræder {stedfortræder} for {kontekst} har ændringer til sin ansættelse, der gør at du bør overveje om personen stadig skal være stedfortræder for dig.";
				break;
			case NEW_MANAGER:
				title = "Mail: Enhed har fået ny leder";
				message = "Kære {modtager},\n<br/>\n<br/>En enhed har fået ny leder.\n<br/>Enhed: {orgenhed}\n<br/>Leder: {medarbejder}\n<br/>Stedfortrædere:\n<br/>{stedfortrædere}";
				break;
			case MANAGER_STOPS:
				title = "Mail: Medarbejder med lederansvar påført ophørsdato";
				message = "Kære {modtager},\n<br/>\n<br/>Medarbejder med lederansvar påført ophørsdato.\n<br/>Enhed: {orgenhed}\n<br/>Medarbejder: {medarbejder}";
				break;
			case MANAGER_REMOVED:
				title = "Mail: Enhed har fået fjernet leder";
				message = "Kære {modtager},\n<br/>\n<br/>En enhed har fået fjernet leder.\n<br/>Enhed: {orgenhed}\n<br/>Tidligere leder: {medarbejder}";
				break;
			case AFFILIATION_EXPIRE_REMINDER:
				title = "Snarligt udløb af tilhørsforhold";
				message = "Kære {modtager}\n<br/>\n<br/>Din medarbejder {medarbejder} har et tilhørsforhold i {orgenhed} som afsluttes om {dage} dage.\n<br/>\n<br/>Medarbejdernummer: {medarbejdernummer}\n<br/>Stopdato: {tidspunkt}\n<br/>Stilling: {stilling}\n<br/>Brugernavn: {kontonavn}";
				daysBeforeEvent = 30;
				break;
			case RESIGNATION:
				title = "Ophør af ansættelse om {dage} dage";
				message = "Kære {modtager}\n<br/>\n<br/>Din medarbejder {medarbejder} har et tilhørsforhold i {orgenhed} som afsluttes om {dage} dage.\n<br/>\n<br/>Medarbejdernummer: {medarbejdernummer}\n<br/>Stopdato: {tidspunkt}\n<br/>Stilling: {stilling}\n<br/>Brugernavn: {kontonavn}";
				daysBeforeEvent = 30;
				break;
			case FUNCTION_NEW_MANAGER:
				title = "En eller flere medarbejdere i {orgenhed} har medarbejderfunktioner";
				message = "Kære {modtager}\n<br/>\n<br/>Enheden {orgenhed} med medarbejderfunktioner har fået ny leder.\n<br/>Medarbejderfunktioner i enheden:\n<br/>{funktionsliste}";
				break;
			case FUNCTION_ASSIGNMENT_EXPIRES:
				title = "Medarbejderfunktion udløber";
				message = "Kære {modtager}\n<br/>\n<br/>Medarbejderen {medarbejder} har medarbejderfunktionen {funktion}, der stopper om {dage} dage.";
				break;
			case FUNCTION_ASSIGNMENT_FOLLOW_UP:
				title = "Opfølgning på medarbejderfunktion";
				message = "Kære {modtager}\n<br/>\n<br/>Medarbejderen {medarbejder} har medarbejderfunktionen {funktion}, der skal laves opfølgning på.";
				break;
			case ORDER_PENDING_APPOVAL:
				title = "Oprettelse af brugerkonto kræver godkendelse";
				message = "Kære {modtager}\n<br/>\n<br/>Der er blevet bestilt en brugerkonto til {medarbejder}, som afventer din godkendelse. Brugerkontoen vil først blive oprettet når du har godkendt bestillingen.";
				break;
			default:
				break;
		}
		
		EmailTemplateChild child = new EmailTemplateChild();
		child.setTitle(title);
		child.setMessage(message);
		child.setEnabled(false);
		child.setEmailTemplate(template);
		child.setDaysBeforeEvent(daysBeforeEvent);
		child.setExcludedOrgUnitMappings(new ArrayList<>());
		child.setEmployeeFilter(EmployeeFilter.ALL);
		return child;
	}

	public EmailTemplate findByTemplateType(EmailTemplateType type) {
		EmailTemplate template = emailTemplateDao.findByTemplateType(type);
		if (template == null) {
			template = new EmailTemplate();
			template.setTemplateType(type);
			template.setChildren(new ArrayList<>());
			template.getChildren().add(generateDefaultChild(template));
			template = emailTemplateDao.save(template);
		}
		
		return template;
	}

	public EmailTemplate save(EmailTemplate template) {
		return emailTemplateDao.save(template);
	}

	public EmailTemplate findById(long id) {
		return emailTemplateDao.findById(id);
	}
	
	// depending on EmailTemplateChild settings, this will either return the manager or all (relevant) substitutes (email addresses that is)
	public List<Person> getManagerOrSubstitutes(EmailTemplateChild child, Person manager, String orgUnitUuid) {
		List<Person> recipients = new ArrayList<>();

		if (child.getSendTo().equals(SendTo.SEND_TO_MANAGER_OR_SUBSTITUTES) || child.getSendTo().equals(SendTo.SEND_TO_MANAGER_AND_SUBSTITUTES)) {
			for (SubstituteAssignment assignment : manager.getSubstitutes()) {
				Person recipient = assignment.getSubstitute();
				if (!StringUtils.hasLength(PersonService.getEmail(recipient))) {
					continue;
				}
				
				switch (assignment.getContext().getIdentifier()) {
					case "GLOBAL":
						recipients.add(recipient);
						break;
					case "SOFD":
						if (orgUnitUuid == null || CollectionUtils.isEmpty(assignment.getConstraintMappings()) || assignment.getConstraintMappings().stream().anyMatch(soum -> Objects.equals(orgUnitUuid, soum.getOrgUnit().getUuid()))) {
							recipients.add(recipient);
						}
						break;
					default:
						// not relevant for SOFD
						break;
				}
			}

			OrgUnit ou = orgUnitService.getByUuid(orgUnitUuid);
			if (ou != null) {
				for (SubstituteOrgUnitAssignment assignment : ou.getSubstitutes()) {
					Person recipient = assignment.getSubstitute();
					if (!StringUtils.hasLength(PersonService.getEmail(recipient))) {
						continue;
					}

					switch (assignment.getContext().getIdentifier()) {
						case "GLOBAL","SOFD":
							recipients.add(recipient);
							break;
						default:
							// not relevant for SOFD
							break;
					}
				}
			}

			if ((recipients.isEmpty() && child.getSendTo().equals(SendTo.SEND_TO_MANAGER_OR_SUBSTITUTES)) || child.getSendTo().equals(SendTo.SEND_TO_MANAGER_AND_SUBSTITUTES)) {
				String email = PersonService.getEmail(manager);

				if (StringUtils.hasLength(email)) {
					recipients.add(manager);
				}
			}
		} else if (child.getSendTo().equals(SendTo.SEND_TO_MANAGER)) {
			String email = PersonService.getEmail(manager);

			if (StringUtils.hasLength(email)) {
				recipients.add(manager);
			}
		}
		

		return recipients;
	}
}
