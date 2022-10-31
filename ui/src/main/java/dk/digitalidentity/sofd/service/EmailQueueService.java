package dk.digitalidentity.sofd.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.transaction.Transactional;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.controller.mvc.dto.InlineImageDTO;
import dk.digitalidentity.sofd.dao.EmailQueueDao;
import dk.digitalidentity.sofd.dao.model.Attachment;
import dk.digitalidentity.sofd.dao.model.EmailQueue;
import dk.digitalidentity.sofd.dao.model.EmailTemplateChild;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.enums.EntityType;
import dk.digitalidentity.sofd.dao.model.enums.EventType;
import dk.digitalidentity.sofd.log.AuditLogger;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EmailQueueService {
	
	@Autowired
	private EmailQueueDao emailQueueDao;
	
	@Autowired
	private EmailService emailService;
	
	@Autowired
	private EboksService eBoksService;
	
	@Autowired
	private AuditLogger auditLogger;
	
	@Autowired
	private PersonService personService;

	@Autowired
	private SofdConfiguration configuration;
	
	public List<EmailQueue> getByCprAndDeliveryTtsAfter(String cpr, Date tts) {
		return emailQueueDao.findAllByCprAndDeliveryTtsAfter(cpr, tts);
	}
	
	public void deleteAllIn(List<EmailQueue> emailQueues) {
		emailQueueDao.deleteAll(emailQueues);
	}

	public void queueEmail(String email, String title, String message, long delay, EmailTemplateChild templateChild) {
		EmailQueue mail = new EmailQueue();
		mail.setEmail(email);
		mail.setMessage(message);
		mail.setTitle(title);
		mail.setDeliveryTts(getDeliveryTts(delay));
		mail.setEmailTemplateChild(templateChild);

		emailQueueDao.save(mail);
	}
	
	public void queueEmail(String title, String message, Date deliveryTts, EmailTemplateChild templateChild, List<Person> persons) {
		for (Person person : persons) {
			String email = PersonService.getEmail(person);
			message = message.replace(EmailTemplateService.RECEIVER_PLACEHOLDER, PersonService.getName(person));
			title = title.replace(EmailTemplateService.RECEIVER_PLACEHOLDER, PersonService.getName(person));

			EmailQueue mail = new EmailQueue();
			//email can be null - means that we will look up mail from uuid when sending
			mail.setEmail(email);
			mail.setPersonUuid(person.getUuid());
			mail.setMessage(message);
			mail.setTitle(title);
			mail.setDeliveryTts(deliveryTts);
			mail.setEmailTemplateChild(templateChild);

			emailQueueDao.save(mail);
		}
	}
	
	public void queueEboks(String cpr, String title, String message, long delay, EmailTemplateChild templateChild) {
		if (!isCpr(cpr)) {
			log.warn("Not sending e-boks to invalid cpr = " + cpr);
			return;
		}

		EmailQueue mail = new EmailQueue();
		mail.setCpr(cpr);
		mail.setMessage(message);
		mail.setTitle(title);
		mail.setDeliveryTts(getDeliveryTts(delay));
		mail.setEmailTemplateChild(templateChild);
		
		emailQueueDao.save(mail);
	}

	public void queueEboks(String title, String message, Date deliveryTts, EmailTemplateChild templateChild, List<Person> persons) {
		for (Person person : persons) {
			if (!isCpr(person.getCpr())) {
				log.warn("Not sending e-boks to invalid cpr = " + person.getCpr());
				continue;
			}
			
			message = message.replace(EmailTemplateService.RECEIVER_PLACEHOLDER, PersonService.getName(person));
			title = title.replace(EmailTemplateService.RECEIVER_PLACEHOLDER, PersonService.getName(person));

			EmailQueue mail = new EmailQueue();
			mail.setCpr(person.getCpr());
			mail.setMessage(message);
			mail.setTitle(title);
			mail.setDeliveryTts(deliveryTts);
			mail.setEmailTemplateChild(templateChild);
			
			emailQueueDao.save(mail);
		}
	}
	
	private boolean isCpr(String cpr) {
		if (cpr == null) {
			return false;
		}
		
		if (cpr.length() != 10) {
			return false;
		}

		try {
			long day = Long.parseLong(cpr.substring(0, 2));
			long month = Long.parseLong(cpr.substring(2, 4));

			if (day < 1 || day > 31 || month < 1 || month > 12) {
				return false;
			}
		}
		catch (Exception ex) {
			log.error("Could not parse cpr: " + cpr, ex);
			return false;
		}

		return true;
	}

	private Date getDeliveryTts(long delay) {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MINUTE, (int) delay);

		return cal.getTime();
	}

	@Transactional
	public void sendPending() {
		List<EmailQueue> emails = findPending();
		
		if (emails.size() > 0) {
			log.info("Found " + emails.size() + " pending emails");
		}

		for (EmailQueue email : emails) {
			EmailTemplateChild templateChild = email.getEmailTemplateChild();
			if (templateChild != null) {
				templateChild.forceLoadAttachments();
			}
			List<Attachment> attachments = (templateChild != null && templateChild.getAttachments() != null && templateChild.getAttachments().size() > 0) ? templateChild.getAttachments() : null;
			
			boolean success = false;
			if (StringUtils.hasLength(email.getCpr())) {
				if (attachments != null) {
					success = eBoksService.sendMessageWithAttachments(email.getCpr(), email.getTitle(), email.getMessage(), attachments);
				}
				else {
					success = eBoksService.sendMessage(email.getCpr(), email.getTitle(), email.getMessage());
				}
				
				if (success) {
					auditLogger.log(null, EntityType.CPR, EventType.MESSAGE_SEND, email.getCpr().substring(0, 6) + "-XXXX", "Eboks besked sendt. Anvendte skabelonen med titlen: " + email.getEmailTemplateChild().getTitle());
				}
			}
			else if (StringUtils.hasLength(email.getEmail()) || StringUtils.hasLength(email.getPersonUuid())) {
				
				if (StringUtils.hasLength(email.getEmail())) {
					// verify that email still exists
					Person person = personService.getByUuid(email.getPersonUuid());
					if (person != null) {
						var emailStillExists = person.getUsers().stream().anyMatch(u -> u.getUser().getUserType().equalsIgnoreCase(SupportedUserTypeService.getExchangeUserType()) && u.getUser().getUserId().equalsIgnoreCase(email.getEmail()));
						if (!emailStillExists) {
							log.warn("Could not find email " + email.getEmail() + " on person with uuid " + person.getUuid() + " when sending email of type " + templateChild.getEmailTemplate().getTemplateType().toString());
							emailQueueDao.delete(email);
							continue;
						}
					}
				}
				else {
					// find an email to send to but fail if one doesn't exist
					Person person = personService.getByUuid(email.getPersonUuid());
					if (person != null) {
						String foundEmail = PersonService.getEmail(person);
						if (!StringUtils.hasLength(foundEmail)) {
							log.warn("Could not find email on person with uuid " + person.getUuid() + " when sending email of type " + templateChild.getEmailTemplate().getTemplateType().toString());
							emailQueueDao.delete(email);
							continue;
						}
						email.setEmail(foundEmail);
					}
				}

				//Check if email matches whitelisted domains
				if (templateChild.getDomainFilter() != null && configuration.getEmailTemplate().isDomainFilterEnabled()) {
					List<String> domains = Arrays.asList(templateChild.getDomainFilter().split(","));
					if (domains.size() > 0 && domains.stream().noneMatch(filter -> email.getEmail().endsWith(filter.trim()))) {
						log.warn("Email " + email.getEmail() + " did not match domain filter " + templateChild.getDomainFilter() + " on email type " + templateChild.getEmailTemplate().getTemplateType().toString());
						emailQueueDao.delete(email);
						continue;
					}
				}

				List<InlineImageDTO> inlineImages = null;
				if (templateChild != null) {
					inlineImages = transformImages(email);
				}

				success = emailService.sendMessage(email.getEmail(), email.getTitle(), email.getMessage(), attachments, inlineImages, templateChild);
				
				if (success) {
					StringBuilder logMessage = new StringBuilder();
					logMessage.append("Email sendt. Anvendte skabelonen med titlen: " + email.getEmailTemplateChild().getTitle());
					
					if (templateChild != null) {
						if (templateChild.getRecipientsCC() != null) {
							logMessage.append(", CC: " + templateChild.getRecipientsCC());
							
						}

						if (templateChild.getRecipientsBCC() != null) {
							logMessage.append(", BCC: " + templateChild.getRecipientsBCC());
						}
					}
					
					auditLogger.log(null, EntityType.EMAIL, EventType.MESSAGE_SEND, email.getEmail(), logMessage.toString());
				}
			}
			else {
				log.error("Cannot send message with id '" + email.getId() + "' due to no cpr/email");
			}
			
			if (success) {
				emailQueueDao.delete(email);
			}
		}
	}
	
	private List<EmailQueue> findPending() {
		Date tts = new Date();
		
		return emailQueueDao.findTop10ByDeliveryTtsBefore(tts);
	}
	
	private List<InlineImageDTO> transformImages(EmailQueue email) {
		List<InlineImageDTO> inlineImages = new ArrayList<>();
		String message = email.getMessage();
		Document doc = Jsoup.parse(message);

		for (Element img : doc.select("img")) {
			String src = img.attr("src");
			if (src == null || src == "") {
				continue;
			}

			InlineImageDTO inlineImageDto = new InlineImageDTO();
			inlineImageDto.setBase64(src.contains("base64"));
			
			if (!inlineImageDto.isBase64()) {
				continue;
			}
			
			String cID = UUID.randomUUID().toString();
			inlineImageDto.setCid(cID);
			inlineImageDto.setSrc(src);
			inlineImages.add(inlineImageDto);
			img.attr("src", "cid:" + cID);
		}

		email.setMessage(doc.html());
		
		return inlineImages;		
	}

}
