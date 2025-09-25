package dk.digitalidentity.sofd.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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
import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.Attachment;
import dk.digitalidentity.sofd.dao.model.EmailQueue;
import dk.digitalidentity.sofd.dao.model.EmailTemplateChild;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.User;
import dk.digitalidentity.sofd.dao.model.enums.EmailTemplatePlaceholder;
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
	private PersonService personService;
	
	@Autowired
	private UserService userService;

	@Autowired
	private SofdConfiguration configuration;
	
	public List<EmailQueue> getByCprAndDeliveryTtsAfter(String cpr, Date tts) {
		return emailQueueDao.findAllByCprAndDeliveryTtsAfter(cpr, tts);
	}
	
	public void deleteAllIn(List<EmailQueue> emailQueues) {
		emailQueueDao.deleteAll(emailQueues);
	}

	public List<EmailQueue> findAll() {
		return emailQueueDao.findAll();
	}

	public EmailQueue findById(long id) {
		return emailQueueDao.findById(id);
	}

	public EmailQueue save(EmailQueue entity) {
		return emailQueueDao.save(entity);
	}

	public void delete(EmailQueue entity) {
		emailQueueDao.delete(entity);
	}

	public void queueEmailToSystemMailbox(String email, String title, String message, long delay, EmailTemplateChild templateChild,String logContext) {
		EmailQueue mail = new EmailQueue();
		mail.setEmail(email);
		mail.setMessage(message);
		mail.setTitle(title);
		mail.setDeliveryTts(getDeliveryTts(delay));
		mail.setEmailTemplateChild(templateChild);
		mail.setPerformEmailCheck(false); // we do not perform checks on email-existance when sending to "external" mailbox
		mail.setRecipient(email);
		mail.setLogContext(logContext);

		emailQueueDao.save(mail);
	}
	
	public void queueEmail(String email, String title, String message, long delay, EmailTemplateChild templateChild, String logContext) {
		queueEmail(email, title, message, delay, templateChild, email, logContext);
	}

	public void queueEmail(Person recipient, String title, String message, long delay, EmailTemplateChild templateChild, String logContext) {
		queueEmail(PersonService.getEmail(recipient), title, message, delay, templateChild, PersonService.getName(recipient), logContext);
	}

	private void queueEmail(String email, String title, String message, long delay, EmailTemplateChild templateChild, String recipient, String logContext) {
		EmailQueue mail = new EmailQueue();
		mail.setEmail(email);
		mail.setMessage(message);
		mail.setTitle(title);
		mail.setDeliveryTts(getDeliveryTts(delay));
		mail.setEmailTemplateChild(templateChild);
		mail.setPerformEmailCheck(true);
		mail.setRecipient(recipient);
		mail.setLogContext(logContext);

		emailQueueDao.save(mail);
	}
	
	public void queueEmail(String title, String message, Date deliveryTts, EmailTemplateChild templateChild, List<Person> persons, String logContext) {
		for (Person person : persons) {
			String email = PersonService.getEmail(person);
			message = message.replace(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER.getPlaceholder(), PersonService.getName(person));
			title = title.replace(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER.getPlaceholder(), PersonService.getName(person));

			EmailQueue mail = new EmailQueue();
			mail.setMessage(message);
			mail.setTitle(title);
			mail.setDeliveryTts(deliveryTts);
			mail.setEmailTemplateChild(templateChild);
			mail.setLogContext(logContext);
			
			// email can be null - means that we will look up mail from uuid when sending
			if (StringUtils.hasLength(email)) {
				mail.setEmail(email);
				mail.setPerformEmailCheck(true);
			}
			else {
				mail.setPersonUuid(person.getUuid());
			}
			mail.setRecipient(PersonService.getName(person));

			emailQueueDao.save(mail);
		}
	}

	// this method is for eboks messages. Here child.getMinutesDelay is days and not minutes :)
	public long getEboksDelay(Person person, EmailTemplateChild child, Affiliation affiliation) {
		long delay = 0;

		if( affiliation == null ) {
			// find an affiliation to base delay on
			affiliation = AffiliationService.notStoppedAffiliations(person.getAffiliations()).stream().min(Comparator.comparing(Affiliation::getStartDate)).orElse(null);
		}

		if (affiliation != null && affiliation.getStartDate() != null) {
			Date today = new Date();

			// child.getMinutesDelay = 0 means that we will send immediately
			if (child.getMinutesDelay() > 0) {
				if (AffiliationService.notActiveYet(affiliation, (int) child.getMinutesDelay())) {
					// the affiliation starts in more than x (child.getMinutesDelay()) days
					Date startDate = affiliation.getStartDate();
					Calendar cal = Calendar.getInstance();
					cal.setTime(startDate);
					cal.add(Calendar.DATE, (int) (-1 * child.getMinutesDelay()));
					Date dateToSend = cal.getTime();

					long diff = dateToSend.getTime() - today.getTime();
					delay = TimeUnit.MILLISECONDS.toMinutes(diff);

					// should not happen
					if (delay < 0) {
						delay = 0;
					}
				}
			}
		}

		return delay;
	}
	
	public void queueEboks(Person person, String title, String message, long delay, EmailTemplateChild templateChild, String logContext) {
		if (!isCpr(person.getCpr())) {
			log.warn("Not sending Digital Post to invalid cpr = " + person.getCpr());
			return;
		}

		EmailQueue mail = new EmailQueue();
		mail.setCpr(person.getCpr());
		mail.setMessage(message);
		mail.setTitle(title);
		mail.setDeliveryTts(getDeliveryTts(delay + configuration.getIntegrations().getEboks().getAdditionalDelayMinutes()));
		mail.setEmailTemplateChild(templateChild);
		mail.setRecipient(PersonService.getName(person));
		mail.setLogContext(logContext);
		
		emailQueueDao.save(mail);
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

			if( templateChild == null ) {
				log.warn("Not sending email with title " + email.getTitle() + ". Email template child was not found - probably deleted after email was queued");
				emailQueueDao.delete(email);
				continue;
			}
			templateChild.forceLoadAttachments();
			List<Attachment> attachments = (templateChild != null && templateChild.getAttachments() != null && templateChild.getAttachments().size() > 0) ? templateChild.getAttachments() : null;
			
			boolean success = false;
			if (StringUtils.hasLength(email.getCpr())) {
				success = eBoksService.sendMessage(email.getCpr(), email.getTitle(), email.getMessage(), attachments, templateChild.isRawTemplate(), email.getLogContext());
			}
			else if (StringUtils.hasLength(email.getEmail()) || StringUtils.hasLength(email.getPersonUuid())) {
				
				if (StringUtils.hasLength(email.getEmail())) {
					// verify that email still exists
					if (email.isPerformEmailCheck()) {
						User user = userService.findByUserIdAndUserType(email.getEmail(), SupportedUserTypeService.getExchangeUserType());
						if (user == null) {
							log.warn("Could not find email " + email.getEmail() + " when sending email of type " + templateChild.getEmailTemplate().getTemplateType().toString());
							emailQueueDao.delete(email);
							continue;
						}
						else if (user.isDisabled()) {
							log.warn("Email " + email.getEmail() + " was disabled when sending email of type " + templateChild.getEmailTemplate().getTemplateType().toString());
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
						
						// TODO: should probably check if the account is active (disabled == 0)
						email.setEmail(foundEmail);
					}
				}

				// check if email matches whitelisted domains
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

				success = emailService.sendMessage(email.getEmail(), email.getTitle(), email.getMessage(), attachments, inlineImages, templateChild, email.getLogContext());
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
