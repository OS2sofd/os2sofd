package dk.digitalidentity.sofd.controller.rest.admin;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.controller.mvc.dto.InlineImageDTO;
import dk.digitalidentity.sofd.controller.rest.admin.model.AttachmentDTO;
import dk.digitalidentity.sofd.controller.rest.admin.model.EmailTemplateChildDTO;
import dk.digitalidentity.sofd.dao.model.Attachment;
import dk.digitalidentity.sofd.dao.model.AttachmentFile;
import dk.digitalidentity.sofd.dao.model.EmailTemplate;
import dk.digitalidentity.sofd.dao.model.EmailTemplateChild;
import dk.digitalidentity.sofd.dao.model.EmailTemplateChildOrgUnitMapping;
import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.User;
import dk.digitalidentity.sofd.dao.model.enums.EmailTemplatePlaceholder;
import dk.digitalidentity.sofd.dao.model.enums.EmailTemplateType;
import dk.digitalidentity.sofd.dao.model.enums.EntityType;
import dk.digitalidentity.sofd.dao.model.enums.EventType;
import dk.digitalidentity.sofd.log.AuditLogger;
import dk.digitalidentity.sofd.security.RequireAdminAccess;
import dk.digitalidentity.sofd.security.SecurityUtil;
import dk.digitalidentity.sofd.service.EboksService;
import dk.digitalidentity.sofd.service.EmailService;
import dk.digitalidentity.sofd.service.EmailTemplateChildService;
import dk.digitalidentity.sofd.service.EmailTemplateService;
import dk.digitalidentity.sofd.service.OrgUnitService;
import dk.digitalidentity.sofd.service.PersonService;
import dk.digitalidentity.sofd.service.SupportedUserTypeService;
import dk.digitalidentity.sofd.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequireAdminAccess
@RestController
public class EmailTemplateRestController {
	
	@Autowired
	private EmailTemplateService emailTemplateService;

	@Autowired
	private AuditLogger auditLogger;

	@Autowired
	private EmailTemplateChildService emailTemplateChildService;
	
	@Autowired
	private EmailService emailService;
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private PersonService personService;

	@Autowired
	private SofdConfiguration configuration;
	
	@Autowired
	private OrgUnitService orgUnitService;

	@Autowired
	private EboksService eboksService;
	
	@Autowired
	private MessageSource messageSource;
	
	@DeleteMapping("/rest/mailtemplates/attachment/{templateChildId}/{attachmentId}")
	public ResponseEntity<Long> uploadAttachment(@PathVariable("templateChildId") Long templateChildId, @PathVariable("attachmentId") Long attachmentId) {
		EmailTemplateChild templateChild = emailTemplateChildService.findById(templateChildId);
		if (templateChild == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		
		boolean changes = false;
		for (Iterator<Attachment> iterator = templateChild.getAttachments().iterator(); iterator.hasNext();) {
			Attachment attachment = iterator.next();
			
			if (attachment.getId() == attachmentId) {
				iterator.remove();
				changes = true;
				break;
			}
		}
		
		if (changes) {
			emailTemplateChildService.save(templateChild);
		}
		
		return new ResponseEntity<>(HttpStatus.OK);
	}
	
	@PostMapping("/rest/mailtemplates/attachment")
	public ResponseEntity<Long> uploadAttachment(@ModelAttribute AttachmentDTO attachment) {
		long id = 0;

		try {
			EmailTemplateChild templateChild = emailTemplateChildService.findById(attachment.getTemplateId());
			if (templateChild == null) {
				return new ResponseEntity<>(HttpStatus.NOT_FOUND);
			}
			
			String filename = attachment.getFile().getOriginalFilename();
			if (!StringUtils.hasLength(filename)) {
				log.warn("No file supplied!");
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}
			
			InputStream is = attachment.getFile().getInputStream();
			byte[] content = IOUtils.toByteArray(is);
			if (content == null || content.length == 0) {
				log.warn("No content supplied!");
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}
			
			Attachment a = new Attachment();
			a.setEmailTemplateChild(templateChild);
			a.setFilename(filename);
			a.setFile(new AttachmentFile());
			a.getFile().setContent(content);
			
			templateChild.getAttachments().add(a);
			templateChild = emailTemplateChildService.save(templateChild);

			// bit of a hack, using the filename... but it seems our only choice when cascade-saving
			for (Attachment att : templateChild.getAttachments()) {
				if (att.getFilename().equals(a.getFilename())) {
					id = att.getId();
				}
			}
		}
		catch (Exception ex) {
			log.error("Bad file", ex);
			
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		return new ResponseEntity<>(id, HttpStatus.OK);
	}

	@PostMapping(value = "/rest/mailtemplates/htmlToPdf")
	public ResponseEntity<byte[]> downloadPdf(@RequestBody EmailTemplateChildDTO emailTemplateChildDTO) {
		try {
			EmailTemplateChild templateChild = emailTemplateChildService.findById(emailTemplateChildDTO.getId());
			if (templateChild == null) {
				EmailTemplate emailTemplate = emailTemplateService.findById(emailTemplateChildDTO.getTemplateId());
				templateChild = emailTemplateService.generateDefaultChild(emailTemplate);
			}
			var message = emailTemplateChildDTO.getMessage();
			// perform placeholder replacement so you can verify that the template replacement worked.
			message = message.replace(EmailTemplatePlaceholder.TITLE.getPlaceholder(), emailTemplateChildDTO.getTitle());
			Locale locale = new Locale("da", "DK");
			for (var placeholder : templateChild.getEmailTemplate().getTemplateType().getEmailTemplatePlaceholders()) {

				try {
					String exampleValue = messageSource.getMessage(placeholder.getExample(), null, locale);
					message = message.replace(placeholder.getPlaceholder(), exampleValue);
				} catch (NoSuchMessageException e) {
					log.warn("Message key not found: " + placeholder.getExample());
				}
			}
			var pdfBytes = eboksService.generatePDF(emailTemplateChildDTO.getTitle(),message, templateChild.isRawTemplate());
			return new ResponseEntity<>(pdfBytes, HttpStatus.OK);
		}
		catch( Exception e) {
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping(value = "/rest/mailtemplates")
	@ResponseBody
	public ResponseEntity<String> updateTemplate(@RequestBody EmailTemplateChildDTO emailTemplateChildDTO, @RequestParam("tryEmail") boolean tryEmail) {
		EmailTemplateChild templateChild;
		if (emailTemplateChildDTO.getId() == 0 && emailTemplateChildDTO.getTemplateId() == 0) {
			EmailTemplate template = new EmailTemplate();
			try {
			    template.setTemplateType(EmailTemplateType.valueOf(emailTemplateChildDTO.getTemplateName()));
			} catch (IllegalArgumentException e) {
			    return ResponseEntity.badRequest().body("Ugyldig template type: " + emailTemplateChildDTO.getTemplateName());
			}
			emailTemplateService.save(template);
			templateChild = emailTemplateService.generateDefaultChild(template);
		}
		else if (emailTemplateChildDTO.getId() == 0) {
			templateChild = emailTemplateService.generateDefaultChild(emailTemplateService.findById(emailTemplateChildDTO.getTemplateId()));
		}
		else {
			templateChild = emailTemplateChildService.findById(emailTemplateChildDTO.getId());
		}

		if (templateChild == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		if( templateChild.getEmailTemplate().getTemplateType().isEboks() ) {
			// verify that html can be converted to pdf without errors
			try {
				eboksService.generatePDF(emailTemplateChildDTO.getTitle(),emailTemplateChildDTO.getMessage(), templateChild.isRawTemplate());
			} catch (IOException e) {
				return new ResponseEntity<>("Fejl: Ugyldig html i skabelonen", HttpStatus.CONFLICT);
            }
        }

		if (tryEmail) {
			User user = userService.findByUserIdAndUserType(SecurityUtil.getUser(), SupportedUserTypeService.getActiveDirectoryUserType());

			if (user != null) {
				Person person = personService.findByUser(user);
				List<Attachment> attachments = null;

				if (templateChild != null) {
					templateChild.forceLoadAttachments();
					attachments = templateChild.getAttachments();
				}
				List<InlineImageDTO> inlineImages = templateChild.getEmailTemplate().getTemplateType().isEboks() ? null : transformImages(emailTemplateChildDTO);

				var message = emailTemplateChildDTO.getMessage();
				Locale locale = new Locale("da", "DK");
				
				// perform placeholder replacement so you can verify that the template replacement worked.
				for (var placeholder : templateChild.getEmailTemplate().getTemplateType().getEmailTemplatePlaceholders()) {
					//finds the example key and replaces it with exampleValue from messages.properties
					try {
						String exampleValue = messageSource.getMessage(placeholder.getExample(), null, locale);
		                message = message.replace(placeholder.getPlaceholder(), exampleValue);
		            } catch (NoSuchMessageException e) {
		                log.warn("Message key not found: " + placeholder.getExample());
		            }
				}

				if (templateChild.getEmailTemplate().getTemplateType().isEboks() && configuration.getIntegrations().getEboks().isEnabled()) {
					eboksService.sendMessage(person.getCpr(),emailTemplateChildDTO.getTitle(),message, attachments, templateChild.isRawTemplate(), "Test af skabelon");
					return new ResponseEntity<>("Digital Post sendt til " + PersonService.getName(person), HttpStatus.OK);
				}
				else {
					Optional<User> oUser = PersonService.getUsers(person).stream()
							.filter(u -> u.getUserType().equals(SupportedUserTypeService.getExchangeUserType()) && u.isPrime())
							.findFirst();
					String email = oUser.isPresent() ? oUser.get().getUserId() : null;
					if (email != null) {
						emailService.sendMessage(email, emailTemplateChildDTO.getTitle(), message, attachments, inlineImages, null, "Test af skabelon");
						return new ResponseEntity<>("Test email sendt til " + email, HttpStatus.OK);
					}
					else {
						return new ResponseEntity<>("Du har ingen email adresse registreret!", HttpStatus.CONFLICT);
					}
				}
			}
		}
		else {
			templateChild.setMinutesDelay(emailTemplateChildDTO.getMinutesDelay());
			templateChild.setMessage(emailTemplateChildDTO.getMessage());
			templateChild.setTitle(emailTemplateChildDTO.getTitle());
			templateChild.setEnabled(emailTemplateChildDTO.isEnabled());

			if (templateChild.getEmailTemplate().getTemplateType().isSendToManager()) {
				templateChild.setSendTo(emailTemplateChildDTO.getSendTo());
			}

			if (templateChild.getEmailTemplate().getTemplateType().isShowTO() || templateChild.isOnlyManualRecipients()) {
				templateChild.setRecipients(emailTemplateChildDTO.getRecipients());
			}

			if (templateChild.getEmailTemplate().getTemplateType().isShowCC() || templateChild.isOnlyManualRecipients()) {
				templateChild.setRecipientsCC(emailTemplateChildDTO.getRecipientsCC());
				templateChild.setRecipientsBCC(emailTemplateChildDTO.getRecipientsBCC());
			}
			
			if (templateChild.getEmailTemplate().getTemplateType().isAllowDaysBeforeEvent()) {
				templateChild.setDaysBeforeEvent(emailTemplateChildDTO.getDaysBeforeEvent());
			}

			if (templateChild.getEmailTemplate().getTemplateType().isShowEmployeeFilter()) {
				templateChild.setEmployeeFilter(emailTemplateChildDTO.getEmployeeFilter());
			}

			if (templateChild.getEmailTemplate().getTemplateType().isShowDomainFilter() && configuration.getEmailTemplate().isDomainFilterEnabled()) {
				templateChild.setDomainFilter(emailTemplateChildDTO.getDomainFilter());
			}
			
			if (templateChild.getEmailTemplate().getTemplateType().isShowOrgFilter() && configuration.getEmailTemplate().isOrgFilterEnabled() && !emailTemplateChildDTO.getOrgUnitUuids().isEmpty()) {
				if (!templateChild.getOrgUnitFilterMappings().isEmpty()) {
					templateChild.getOrgUnitFilterMappings().clear();
				}
				
				for (String uuid : emailTemplateChildDTO.getOrgUnitUuids()) {
					OrgUnit ou = orgUnitService.getByUuid(uuid);
					if (ou == null) {
						continue;
					}
					templateChild.getOrgUnitFilterMappings().add(new EmailTemplateChildOrgUnitMapping(templateChild, ou));
				}
				templateChild.setOrgUnitFilterType(emailTemplateChildDTO.getOrgUnitFilterType());
			}
			
			if (templateChild.getEmailTemplate().getTemplateType().isShowADUserFilter() && configuration.getEmailTemplate().isAdUserFilterEnabled()) {
				templateChild.setAdRequired(emailTemplateChildDTO.isAdRequired());
			}

			if (templateChild.getEmailTemplate().getTemplateType().isShowTO()) {
				templateChild.setOnlyManualRecipients(emailTemplateChildDTO.isOnlyManualRecipients());
			}

			emailTemplateChildService.save(templateChild);

			String message = "Mailskabalon oprettet/redigeret: " + templateChild.getEmailTemplate().getTemplateType().getMessage() + " " + templateChild.getTitle() + ".";
			auditLogger.log(String.valueOf(templateChild.getId()), EntityType.EMAIL_TEMPLATE, EventType.SAVE, templateChild.getTitle(), message);
		}



		return new ResponseEntity<>(HttpStatus.OK);
	}
	
	@PostMapping(value = "/rest/mailtemplates/deletechild/{id}")
	public RedirectView deleteChild(@PathVariable("id") Long childId, RedirectAttributes redirectAttributes) {
		EmailTemplateChild templateChild = emailTemplateChildService.findById(childId);
		if (templateChild == null) {
			//return new ResponseEntity<>(HttpStatus.NOT_FOUND);
			redirectAttributes.addFlashAttribute("error", "Template child not found");
			return new RedirectView("#");

		}
		
		emailTemplateChildService.delete(templateChild);
		
		//return new ResponseEntity<>(HttpStatus.OK);
		redirectAttributes.addFlashAttribute("success", "Template successfully deactivated");
		return new RedirectView("/ui/admin/mailtemplates");
	}
	
	private List<InlineImageDTO> transformImages(EmailTemplateChildDTO emailTemplateChildDTO) {
		List<InlineImageDTO> inlineImages = new ArrayList<>();
		String message = emailTemplateChildDTO.getMessage();
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

		emailTemplateChildDTO.setMessage(doc.html());
		
		return inlineImages;
	}

	@PostMapping("/rest/mailtemplates/activatechild/{id}")
	public RedirectView activeCheld(@PathVariable("id") Long childId, RedirectAttributes redirectAttributes) {
		EmailTemplateChild templateChild = emailTemplateChildService.findById(childId);
		if (templateChild == null) {
			redirectAttributes.addFlashAttribute("error", "Template child not found");
			return new RedirectView("/ui/admin/mailtemplates");
		}

		emailTemplateChildService.active(templateChild);
		redirectAttributes.addFlashAttribute("success", "Template successfully activated");
		return new RedirectView("/ui/admin/mailtemplates");
	}

	@PostMapping("/rest/mailtemplates/deactivatechild/{id}")
	public RedirectView deactiveCheld(@PathVariable("id") Long childId, RedirectAttributes redirectAttributes) {
		EmailTemplateChild templateChild = emailTemplateChildService.findById(childId);
		if (templateChild == null) {
			redirectAttributes.addFlashAttribute("error", "Template child not found");
			return new RedirectView("/ui/admin/mailtemplates");
		}

		emailTemplateChildService.deactive(templateChild);
		redirectAttributes.addFlashAttribute("success", "Template successfully deactivated");
		return new RedirectView("/ui/admin/mailtemplates");
	}
}
