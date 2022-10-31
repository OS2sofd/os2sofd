package dk.digitalidentity.sofd.service;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.dao.model.Attachment;
import dk.digitalidentity.sofd.dao.model.Notification;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.enums.EntityType;
import dk.digitalidentity.sofd.dao.model.enums.NotificationType;
import dk.digitalidentity.sofd.service.eboks.dto.EboksAttachment;
import dk.digitalidentity.sofd.service.eboks.dto.EboksMessage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EboksService {

	@Autowired
	private SofdConfiguration configuration;
	
	@Autowired
	private TemplateEngine templateEngine;
	
	@Autowired
	private NotificationService notificationService;

	@Autowired
	private PersonService personService;
	
	public boolean sendMessage(String cpr, String subject, String message) {
		return sendMessageWithAttachments(cpr, subject, message, null);
	}
	
	public boolean sendMessageWithAttachments(String cpr, String subject, String message, List<Attachment> attachments) {
		if (!configuration.getIntegrations().getEboks().isEnabled()) {
			log.warn("e-boks server is not configured - not sending digital post!");
			
			// return true to get the message removed from the queue
			return true;
		}

		if (PersonService.isFictionalCpr(cpr)) {
			log.warn("Person has a fictive cpr - not sending digital post: " + cpr);

			// return true to get the message removed from the queue
			return true;
		}
		
		if (configuration.getIntegrations().getEboks().isNewUrlEnabled()) {
			if (!StringUtils.hasLength(configuration.getIntegrations().getEboks().getSenderName())) {
				log.error("Bad configuration: missing senderName");
				return false;
			}
		}

		RestTemplate restTemplate = new RestTemplate();
		String resourceUrl = (configuration.getIntegrations().getEboks().isNewUrlEnabled()) ? configuration.getIntegrations().getEboks().getUrl() : configuration.getIntegrations().getEboks().getOldUrl();

		log.info("Sending e-boks message: '" + subject + "' to " + PersonService.maskCpr(cpr) + ". Using resourceUrl: " + resourceUrl);

		try {
			EboksMessage eBoks = new EboksMessage();
			eBoks.setCpr(cpr);
			eBoks.setCvr(configuration.getCustomer().getCvr());
			eBoks.setSubject(subject);
			eBoks.setAttachments(new ArrayList<>());

			if (attachments != null && attachments.size() > 0) {
				for (Attachment attachment : attachments) {
					EboksAttachment eboksAttachment = new EboksAttachment();
					eboksAttachment.setFilename(attachment.getFilename());
					eboksAttachment.setContent(Base64.getEncoder().encodeToString(attachment.getFile().getContent()));

					eBoks.getAttachments().add(eboksAttachment);
				}
			}

			if (configuration.getIntegrations().getEboks().isNewUrlEnabled()) {
				eBoks.setContent(Base64.getEncoder().encodeToString(generatePDF(subject, message)));
				eBoks.setMunicipalityName(configuration.getIntegrations().getEboks().getSenderName());			
			}
			else {
				eBoks.setPdfFileBase64(Base64.getEncoder().encodeToString(generatePDF(subject, message)));
				eBoks.setSenderId(configuration.getIntegrations().getEboks().getSenderId());
				eBoks.setContentTypeId(configuration.getIntegrations().getEboks().getMaterialeId());				
			}

	    	HttpHeaders headers = new HttpHeaders();
	        headers.add("Content-Type", "application/json");
			HttpEntity<EboksMessage> request = new HttpEntity<EboksMessage>(eBoks, headers);

			ResponseEntity<String> response = restTemplate.postForEntity(resourceUrl, request, String.class);
			
			if (response.getStatusCodeValue() != 200) {
				log.error("Failed to send e-boks message to: " + PersonService.maskCpr(cpr) + ". HTTP: " + response.getStatusCodeValue());
				return false;
			}
			else {
				if ("Ikke tilmeldt e-boks!".equalsIgnoreCase(response.getBody())) {
					createNotSubscribedNotification(cpr);
				}
			}
		}
		catch (HttpStatusCodeException ex) {
			// when not "tilmeldt" e-boks, just skip sending this one
			if (!"Ikke tilmeldt e-boks!".equalsIgnoreCase(ex.getResponseBodyAsString())) {
				log.error("Failed to send e-boks message to: " + PersonService.maskCpr(cpr), ex);
				return false;
			}
			else {
				createNotSubscribedNotification(cpr);
			}
		}
		
		return true;
	}
	
	private void createNotSubscribedNotification(String cpr) {
		Person person = personService.findByCpr(cpr);

		if (person != null) {
			Notification notification = new Notification();
			notification.setActive(true);
			notification.setAffectedEntityName(PersonService.getName(person));
			notification.setAffectedEntityType(EntityType.PERSON);
			notification.setAffectedEntityUuid(person.getUuid());
			notification.setMessage("Personen er ikke tilmeldt e-boks, og beskeden omkring oprettet AD konto er derfor ikke sendt til medarbejderen");
			notification.setNotificationType(NotificationType.EBOKS_REJECTED);
			notification.setCreated(new Date());

			notificationService.save(notification);
		}
	}

	private byte[] generatePDF(String subject, String message) {
		Context ctx = new Context();
		ctx.setVariable("subject", subject);
		ctx.setVariable("message", message);

		String htmlContent = templateEngine.process("pdf/template", ctx);

		// Create PDF document and return as byte[]
		try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			ITextRenderer renderer = new ITextRenderer();
			renderer.setDocumentFromString(htmlContent);
			renderer.layout();
			renderer.createPDF(outputStream);

			return outputStream.toByteArray();
		}
		catch (Exception ex) {
			log.error("Failed to generate pdf", ex);
			return null;
		}
	}
}
