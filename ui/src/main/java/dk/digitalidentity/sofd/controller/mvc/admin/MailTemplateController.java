package dk.digitalidentity.sofd.controller.mvc.admin;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.controller.rest.admin.model.AttachmentDTO;
import dk.digitalidentity.sofd.controller.rest.admin.model.EmailTemplateChildDTO;
import dk.digitalidentity.sofd.controller.rest.admin.model.EmailTemplateDTO;
import dk.digitalidentity.sofd.dao.model.Attachment;
import dk.digitalidentity.sofd.dao.model.EmailTemplate;
import dk.digitalidentity.sofd.dao.model.EmailTemplateChild;
import dk.digitalidentity.sofd.security.RequireAdminAccess;
import dk.digitalidentity.sofd.service.EmailTemplateChildService;
import dk.digitalidentity.sofd.service.EmailTemplateService;
import dk.digitalidentity.sofd.service.OrgUnitService;
import dk.digitalidentity.sofd.service.model.OUTreeForm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Slf4j
@RequireAdminAccess
@Controller
public class MailTemplateController {

	@Autowired
	private EmailTemplateService emailTemplateService;
	
	@Autowired
	private EmailTemplateChildService emailTemplateChildService;
	
	@Autowired
	private SofdConfiguration configuration;
	
	@Autowired
	private OrgUnitService orgUnitService;

	@Autowired
	private MessageSource messageSource;

	@GetMapping("/ui/admin/mailtemplates")
	public String listTemplates(Model model) {
		model.addAttribute("templates", emailTemplateChildService.findAll().stream().map(EmailTemplateChildDTO::new).toList());
		return "admin/emailtemplate/list";
	}
	
	@GetMapping("/ui/admin/mailtemplates/new")
	public String newTemplate(Model model, Locale locale) {
		List<EmailTemplate> templates = emailTemplateService.getAll();

		if (!configuration.getModules().getAccountCreation().isEnabled()) {
			templates = templates.stream()
					.filter(t -> !t.getTemplateType().isRequireAccountCreation())
					.collect(Collectors.toList());
		}
		
		if (!configuration.getModules().getAccountCreation().isAccountOrderApprove()) {
			templates = templates.stream()
					.filter(t -> !t.getTemplateType().isRequireAccountCreationApproval())
					.collect(Collectors.toList());			
		}

		if (!configuration.getModules().getSubstitute().isEnabled()) {
			templates = templates.stream()
					.filter(t -> !t.getTemplateType().isRequireSubstitute())
					.collect(Collectors.toList());
		}

		if (!configuration.getModules().getFunctionHierarchy().isEnabled()) {
			templates = templates.stream()
					.filter(t -> !t.getTemplateType().isRequireFunctionHierarchy())
					.collect(Collectors.toList());
		}
		
		List<EmailTemplateDTO> templateDTOs = new ArrayList<>();
		for (EmailTemplate template : templates) {
			EmailTemplateDTO emailTemplateDTO = new EmailTemplateDTO();
			emailTemplateDTO.setId(template.getId());
			emailTemplateDTO.setEmailTemplateType(template.getTemplateType());
			emailTemplateDTO.setTemplateTypeText(template.getTemplateType().getMessage());
			
			List<EmailTemplateChildDTO> templateChildDTOs = new ArrayList<>();
			EmailTemplateChildDTO childDto = new EmailTemplateChildDTO(emailTemplateService.generateDefaultChild(template));
			List<AttachmentDTO> attachmentDTOs = new ArrayList<>();
			childDto.setAttachments(attachmentDTOs);
			templateChildDTOs.add(childDto);
			emailTemplateDTO.setChildren(templateChildDTOs);
			templateDTOs.add(emailTemplateDTO);
		}
		templateDTOs.sort((t1,t2) -> messageSource.getMessage(t1.getTemplateTypeText(),null,locale).compareTo(messageSource.getMessage(t2.getTemplateTypeText(),null,locale)));
		model.addAttribute("templates", templateDTOs);
		
		List<OUTreeForm> allOus = orgUnitService.getAllTree();
		model.addAttribute("allOus", allOus);
		model.addAttribute("isNew", true);
		return "admin/emailtemplate/edit";
	}
	
	@GetMapping("/ui/admin/mailtemplates/edit")
	public String editTemplate(Model model, Locale locale) {
		List<EmailTemplate> templates = emailTemplateService.getAll();

		if (!configuration.getModules().getAccountCreation().isEnabled()) {
			templates = templates.stream()
					.filter(t -> !t.getTemplateType().isRequireAccountCreation())
					.collect(Collectors.toList());
		}
		
		if (!configuration.getModules().getAccountCreation().isAccountOrderApprove()) {
			templates = templates.stream()
					.filter(t -> !t.getTemplateType().isRequireAccountCreationApproval())
					.collect(Collectors.toList());			
		}

		if (!configuration.getModules().getSubstitute().isEnabled()) {
			templates = templates.stream()
					.filter(t -> !t.getTemplateType().isRequireSubstitute())
					.collect(Collectors.toList());
		}

		if (!configuration.getModules().getFunctionHierarchy().isEnabled()) {
			templates = templates.stream()
					.filter(t -> !t.getTemplateType().isRequireFunctionHierarchy())
					.collect(Collectors.toList());
		}
		
		List<EmailTemplateDTO> templateDTOs = new ArrayList<>();
		for (EmailTemplate template : templates) {
			EmailTemplateDTO emailTemplateDTO = new EmailTemplateDTO();
			emailTemplateDTO.setId(template.getId());
			emailTemplateDTO.setEmailTemplateType(template.getTemplateType());
			emailTemplateDTO.setTemplateTypeText(template.getTemplateType().getMessage());
			
			List<EmailTemplateChildDTO> templateChildDTOs = new ArrayList<>();
			for (EmailTemplateChild child : template.getChildren()) {
				EmailTemplateChildDTO childDto = new EmailTemplateChildDTO(child);
				List<AttachmentDTO> attachmentDTOs = new ArrayList<>();

				if (child.getAttachments() != null && !child.getAttachments().isEmpty()) {
					for (Attachment attachment : child.getAttachments()) {
						AttachmentDTO attachmentDTO = new AttachmentDTO();
						attachmentDTO.setFilename(attachment.getFilename());
						attachmentDTO.setId(attachment.getId());
						
						attachmentDTOs.add(attachmentDTO);
					}
				}

				childDto.setAttachments(attachmentDTOs);
				templateChildDTOs.add(childDto);
			}

			emailTemplateDTO.setChildren(templateChildDTOs);
			templateDTOs.add(emailTemplateDTO);
		}
		templateDTOs.sort((t1,t2) -> messageSource.getMessage(t1.getTemplateTypeText(),null,locale).compareTo(messageSource.getMessage(t2.getTemplateTypeText(),null,locale)));
		model.addAttribute("templates", templateDTOs);
		
		List<OUTreeForm> allOus = orgUnitService.getAllTree();
		model.addAttribute("allOus", allOus);
		model.addAttribute("isNew", false);

		return "admin/emailtemplate/edit";
	}
	
	@GetMapping("/ui/admin/mailtemplates/addchild/{templateId}")
	public String addMail(Model model, @PathVariable("templateId") long templateId) {
		EmailTemplate template = emailTemplateService.findById(templateId);
		if (template == null) {
			log.warn("Could not find type: " + templateId);
			return "redirect:/ui/admin/mailtemplates";
		}

		// need to save to get ID
		EmailTemplateChild child = emailTemplateService.generateDefaultChild(template);
		child = emailTemplateChildService.save(child);

		template.getChildren().add(child);
		emailTemplateService.save(template);

		return "redirect:/ui/admin/mailtemplates/new?childId=" + child.getId() + "&templateId=" + template.getId();
	}
	
	
}
