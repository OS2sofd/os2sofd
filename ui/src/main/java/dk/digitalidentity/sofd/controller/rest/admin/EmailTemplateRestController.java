package dk.digitalidentity.sofd.controller.rest.admin;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.htmlcleaner.BrowserCompactXmlSerializer;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
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

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.controller.mvc.dto.InlineImageDTO;
import dk.digitalidentity.sofd.controller.rest.admin.model.AttachmentDTO;
import dk.digitalidentity.sofd.controller.rest.admin.model.EmailTemplateChildDTO;
import dk.digitalidentity.sofd.dao.model.Attachment;
import dk.digitalidentity.sofd.dao.model.AttachmentFile;
import dk.digitalidentity.sofd.dao.model.EmailTemplateChild;
import dk.digitalidentity.sofd.dao.model.EmailTemplateChildOrgUnitMapping;
import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.User;
import dk.digitalidentity.sofd.security.RequireAdminAccess;
import dk.digitalidentity.sofd.security.SecurityUtil;
import dk.digitalidentity.sofd.service.EmailService;
import dk.digitalidentity.sofd.service.EmailTemplateChildService;
import dk.digitalidentity.sofd.service.OrgUnitService;
import dk.digitalidentity.sofd.service.PersonService;
import dk.digitalidentity.sofd.service.SupportedUserTypeService;
import dk.digitalidentity.sofd.service.UserService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequireAdminAccess
@RestController
public class EmailTemplateRestController {
	
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

	@PostMapping(value = "/rest/mailtemplates")
	@ResponseBody
	public ResponseEntity<String> updateTemplate(@RequestBody EmailTemplateChildDTO emailTemplateChildDTO, @RequestParam("tryEmail") boolean tryEmail) {
		toXHTML(emailTemplateChildDTO);
		
		if (tryEmail) {
			User user = userService.findByUserIdAndUserType(SecurityUtil.getUser(), SupportedUserTypeService.getActiveDirectoryUserType());

			if (user != null) {
				Person person = personService.findByUser(user);
				
				Optional<User> oUser = PersonService.getUsers(person).stream()
							.filter(u -> u.getUserType().equals(SupportedUserTypeService.getExchangeUserType()) && u.isPrime())
							.findFirst();
				
				String email = oUser.isPresent() ? oUser.get().getUserId() : null;
				if (email != null) {
					List<InlineImageDTO> inlineImages = transformImages(emailTemplateChildDTO);
					List<Attachment> attachments = null;

					EmailTemplateChild templateChild = emailTemplateChildService.findById(emailTemplateChildDTO.getId());
					if (templateChild != null) {
						templateChild.forceLoadAttachments();
						attachments = templateChild.getAttachments();
					}

					emailService.sendMessage(email, emailTemplateChildDTO.getTitle(), emailTemplateChildDTO.getMessage(), attachments, inlineImages, null);
					
					return new ResponseEntity<>("Test email sent til " + email, HttpStatus.OK);
				}
			}
			
			return new ResponseEntity<>("Du har ingen email adresse registreret!", HttpStatus.CONFLICT);
		}
		else {
			EmailTemplateChild templateChild = emailTemplateChildService.findById(emailTemplateChildDTO.getId());
			if (templateChild == null) {
				return new ResponseEntity<>(HttpStatus.NOT_FOUND);
			}

			templateChild.setMinutesDelay(emailTemplateChildDTO.getMinutesDelay());
			templateChild.setMessage(emailTemplateChildDTO.getMessage());
			templateChild.setTitle(emailTemplateChildDTO.getTitle());
			templateChild.setEnabled(emailTemplateChildDTO.isEnabled());

			if (templateChild.getEmailTemplate().getTemplateType().isSendToManager()) {
				templateChild.setSendTo(emailTemplateChildDTO.getSendTo());
			}

			if (templateChild.getEmailTemplate().getTemplateType().isShowTO()) {
				templateChild.setRecipients(emailTemplateChildDTO.getRecipients());
			}

			if (templateChild.getEmailTemplate().getTemplateType().isShowCC()) {
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
				if (!templateChild.getExcludedOrgUnitMappings().isEmpty()) {
					templateChild.getExcludedOrgUnitMappings().clear();
				}
				
				for (String uuid : emailTemplateChildDTO.getOrgUnitUuids()) {
					OrgUnit ou = orgUnitService.getByUuid(uuid);
					if (ou == null) {
						continue;
					}
					templateChild.getExcludedOrgUnitMappings().add(new EmailTemplateChildOrgUnitMapping(templateChild, ou));
				}
			}
			
			if (templateChild.getEmailTemplate().getTemplateType().isShowADUserFilter() && configuration.getEmailTemplate().isAdUserFilterEnabled()) {
				templateChild.setAdRequired(emailTemplateChildDTO.isAdRequired());
			}

			if (templateChild.getEmailTemplate().getTemplateType().isOnlyManualRecipients()) {
				templateChild.setOnlyManualRecipients(emailTemplateChildDTO.isOnlyManualRecipients());
			}

			emailTemplateChildService.save(templateChild);
		}
		
		return new ResponseEntity<>(HttpStatus.OK);
	}
	
	@PostMapping(value = "/rest/mailtemplates/deletechild/{id}")
	public ResponseEntity<Long> deleteChild(@PathVariable("id") Long childId) {
		EmailTemplateChild templateChild = emailTemplateChildService.findById(childId);
		if (templateChild == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		
		emailTemplateChildService.delete(templateChild);
		
		return new ResponseEntity<>(HttpStatus.OK);
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

	/**
	 * summernote does not generate valid XHTML. At least the <br/> and <img/> tags are not closed,
	 * so we need to close them, otherwise our PDF processing will fail.
	 */
	private void toXHTML(EmailTemplateChildDTO emailTemplateChildDTO) {
		String message = emailTemplateChildDTO.getMessage();
		if (message != null) {
			try {
				CleanerProperties properties = new CleanerProperties();
				properties.setOmitXmlDeclaration(true);
				TagNode tagNode = new HtmlCleaner(properties).clean(message);
			
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				new BrowserCompactXmlSerializer(properties).writeToStream(tagNode, bos);
	
				emailTemplateChildDTO.setMessage(new String(bos.toByteArray(), Charset.forName("UTF-8")));
			}
			catch (IOException ex) {
				log.error("could not parse: " + emailTemplateChildDTO.getMessage());
			}
		}
	}
}
