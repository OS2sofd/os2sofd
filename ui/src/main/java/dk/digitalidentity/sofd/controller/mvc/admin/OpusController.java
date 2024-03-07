package dk.digitalidentity.sofd.controller.mvc.admin;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.controller.mvc.admin.dto.OpusAutoAffiliationCreate;
import dk.digitalidentity.sofd.controller.mvc.admin.dto.OpusAutoAffiliationMapping;
import dk.digitalidentity.sofd.controller.mvc.admin.dto.OpusAutoAffiliationPayload;
import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.Setting;
import dk.digitalidentity.sofd.security.RequireAdminAccess;
import dk.digitalidentity.sofd.service.OrgUnitService;
import dk.digitalidentity.sofd.service.SettingService;

@RequireAdminAccess
@Controller
public class OpusController {

	@Autowired
	private SofdConfiguration configuration;
	
	@Autowired
	private SettingService settingService;
	
	@Autowired
	private OrgUnitService orgUnitService;

	@GetMapping(value = "/ui/admin/opusfilter")
	public String opusFilter(Model model) {
		model.addAttribute("losIds", configuration.getIntegrations().getOpus().getLosIds());
		model.addAttribute("positionIds", configuration.getIntegrations().getOpus().getPositionIds());
		model.addAttribute("positionNames", configuration.getIntegrations().getOpus().getPositionNames());
		model.addAttribute("invalidPositionNames", configuration.getIntegrations().getOpus().getInvalidPositionNames());
		model.addAttribute("orgUnitInfix", configuration.getIntegrations().getOpus().getOrgUnitInfix());

		return "admin/opus/filter";
	}	
	
	@GetMapping(value = "/ui/admin/opusautoaff")
	public String opusAutoAffiliations(Model model) {
		Setting autoAffiliations = settingService.getOpusAutoAffiliations();

		List<OrgUnit> orgUnits = orgUnitService.getAllActive();

		List<OpusAutoAffiliationMapping> mappings = new ArrayList<>();
		String[] tokens = autoAffiliations.getValue().split(";");
		for (String token : tokens) {
			String[] pair = token.split("=");
			if (pair.length == 2) {
				OpusAutoAffiliationMapping mapping = new OpusAutoAffiliationMapping();
				mapping.setFromUuid(pair[0]);
				mapping.setToUuid(pair[1]);
				
				OrgUnit ou = findOrgUnit(mapping.getFromUuid(), orgUnits);
				if (ou != null) {
					mapping.setFromName(ou.getName());
				}
				else {
					continue;
				}
				
				ou = findOrgUnit(mapping.getToUuid(), orgUnits);
				if (ou != null) {
					mapping.setToName(ou.getName());
					
					mappings.add(mapping);
				}
			}
		}

		model.addAttribute("mappings", mappings);

		return "admin/opus/autoaffiliation";
	}
	
	@PostMapping(value = "/ui/admin/opusautoaff")
	public String opusAutoAffiliations(@ModelAttribute("payload") OpusAutoAffiliationPayload payload) {
		Setting setting = settingService.getOpusAutoAffiliations();
		setting.setValue(payload.getPayload());
		settingService.save(setting);

		return "redirect:/ui/admin/opusautoaff";
	}

	@GetMapping(value = "/ui/admin/opusautoaff/create")
	public String opusAutoAffiliationsCreate(Model model) {
		model.addAttribute("form", new OpusAutoAffiliationCreate());
		
		return "admin/opus/autoaffiliationcreate";
	}

	@PostMapping(value = "/ui/admin/opusautoaff/create")
	public String handleOpusAutoAffiliationsCreate(@ModelAttribute("form") OpusAutoAffiliationCreate form) {
		Setting setting = settingService.getOpusAutoAffiliations();
		List<OpusAutoAffiliationMapping> mappings = getMappingsWithoutNames(setting.getValue());

		boolean found = false;
		for (OpusAutoAffiliationMapping mapping : mappings) {
			if (mapping.getFromUuid().equals(form.getFromUuid()) && mapping.getToUuid().equals(form.getToUuid())) {
				found = true;
				break;
			}
		}

		if (!found) {
			if (setting.getValue().length() > 0) {
				setting.setValue(setting.getValue() + ";" + form.getFromUuid() + "=" + form.getToUuid());
			}
			else {
				setting.setValue(form.getFromUuid() + "=" + form.getToUuid());
			}
			settingService.save(setting);
		}

		return "redirect:/ui/admin/opusautoaff";
	}

	private OrgUnit findOrgUnit(String uuid, List<OrgUnit> orgUnits) {
		for (OrgUnit ou : orgUnits) {
			if (ou.getUuid().equals(uuid)) {
				return ou;
			}
		}
		
		return null;
	}
	
	private List<OpusAutoAffiliationMapping> getMappingsWithoutNames(String rawMappings) {
		List<OpusAutoAffiliationMapping> mappings = new ArrayList<>();
		String[] tokens = rawMappings.split(";");
		for (String token : tokens) {
			String[] pair = token.split("=");
			if (pair.length == 2) {
				OpusAutoAffiliationMapping mapping = new OpusAutoAffiliationMapping();
				mapping.setFromUuid(pair[0]);
				mapping.setToUuid(pair[1]);
				
				mappings.add(mapping);
			}
		}
		
		return mappings;
	}
}
