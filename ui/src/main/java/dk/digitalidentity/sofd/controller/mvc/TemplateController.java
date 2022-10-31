package dk.digitalidentity.sofd.controller.mvc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import dk.digitalidentity.sofd.controller.mvc.dto.TemplateDTO;
import dk.digitalidentity.sofd.dao.model.Template;
import dk.digitalidentity.sofd.security.RequireSMSAccess;
import dk.digitalidentity.sofd.service.TemplateService;

@RequireSMSAccess
@Controller
public class TemplateController {

	@Autowired
	private TemplateService templateService;

	@GetMapping("/ui/template/list")
	public String list(Model model) {
		model.addAttribute("templates", templateService.findAll());
		
		return "template/list";
	}

	@GetMapping("/ui/template/new")
	public String newTemplate(Model model) {
		model.addAttribute("template", new TemplateDTO());

		return "template/new";
	}

	@PostMapping("/ui/template/new")
	public String newTemplatePost(Model model, @ModelAttribute("template") TemplateDTO templateDTO) {
		Template template = new Template();
		template.setName(templateDTO.getName());
		template.setMessage(templateDTO.getMessage());

		templateService.save(template);

		return "redirect:/ui/template/list";
	}

	@GetMapping("/ui/template/edit/{templateId}")
	public String editTemplate(Model model, @PathVariable long templateId) {
		Template template = templateService.findById(templateId);
		if (template == null) {
			return "redirect:/ui/template/list";
		}

		TemplateDTO templateDTO = new TemplateDTO();
		templateDTO.setId(template.getId());
		templateDTO.setName(template.getName());
		templateDTO.setMessage(template.getMessage());

		model.addAttribute("template", templateDTO);

		return "template/edit";
	}

	@PostMapping("/ui/template/edit")
	public String editTemplate(Model model, @ModelAttribute("template") TemplateDTO templateDTO) {
		Template template = templateService.findById(templateDTO.getId());
		if (template == null) {
			return "redirect:/ui/template/list";
		}

		template.setName(templateDTO.getName());
		template.setMessage(templateDTO.getMessage());

		template = templateService.save(template);

		return "redirect:/ui/template/list";
	}

	@GetMapping("/ui/template/delete/{templateId}")
	public String removeTemplate(@PathVariable long templateId) {
		Template template = templateService.findById(templateId);
		if (template != null) {
			templateService.delete(template);
		}

		return "redirect:/ui/template/list";
	}
}
