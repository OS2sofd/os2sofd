package dk.digitalidentity.sofd.service;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
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
	private int errorCount = 0;

	@Autowired
	private SofdConfiguration configuration;
	
	@Autowired
	private TemplateEngine templateEngine;
	
	@Autowired
	private NotificationService notificationService;

	@Autowired
	private PersonService personService;
	
	public boolean sendMessage(String cpr, String subject, String message, boolean rawTemplate) {
		return sendMessageWithAttachments(cpr, subject, message, null, rawTemplate);
	}
	
	public boolean sendMessageWithAttachments(String cpr, String subject, String message, List<Attachment> attachments, boolean rawTemplate) {
		if (!configuration.getIntegrations().getEboks().isEnabled()) {
			log.warn("e-boks server is not configured - not sending digital post!");
			
			// return true to get the message removed from the queue
			return true;
		}

		var person = personService.findByCpr(cpr);
		if (person == null || person.isDead()) {
			log.warn("Person not found or person is dead - not sending digital post: " + cpr);
			// return true to get the message removed from the queue
			return true;
		}

		if (PersonService.isFictionalCpr(cpr)) {
			log.warn("Person has a fictive cpr - not sending digital post: " + cpr);

			// return true to get the message removed from the queue
			return true;
		}
		
		if (!StringUtils.hasLength(configuration.getIntegrations().getEboks().getSenderName())) {
			log.error("Bad configuration: missing senderName");
			return false;
		}

		RestTemplate restTemplate = new RestTemplate();
		String resourceUrl = configuration.getIntegrations().getEboks().getUrl();

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

			byte[] pdf = generatePDF(subject, message, (rawTemplate && configuration.getEmailTemplate().isRawTemplatesSupported()));
			eBoks.setContent(Base64.getEncoder().encodeToString(pdf));
			eBoks.setMunicipalityName(configuration.getIntegrations().getEboks().getSenderName());

	    	HttpHeaders headers = new HttpHeaders();
	        headers.add("Content-Type", "application/json");
			HttpEntity<EboksMessage> request = new HttpEntity<EboksMessage>(eBoks, headers);

			restTemplate.postForEntity(resourceUrl, request, String.class);
		}
		catch (HttpStatusCodeException ex) {
			if (ex.getRawStatusCode() == 409) {
				createNotSubscribedNotification(cpr);
			}
			else {
				errorCount++;

				if (errorCount > 5) {
					log.error("Failed to send e-boks message to: " + PersonService.maskCpr(cpr), ex);
				}
				else {
					log.warn("Failed to send e-boks message to: " + PersonService.maskCpr(cpr), ex);
				}

				return false;
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

	private byte[] generatePDF(String subject, String message, boolean raw) {
		Context ctx = new Context();
		ctx.setVariable("subject", subject);
		ctx.setVariable("message", message);

		String htmlContent = null;
		if (raw) {
			htmlContent = templateEngine.process("pdf/template_raw", ctx);
		}
		else {
			htmlContent = templateEngine.process("pdf/template", ctx);
		}

		htmlContent = htmlContent.replace("&nbsp;", "&#160;");
		htmlContent = htmlContent.replace("<br>", "<br />");
		htmlContent = htmlContent.replace("& ", "&amp; ");

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
