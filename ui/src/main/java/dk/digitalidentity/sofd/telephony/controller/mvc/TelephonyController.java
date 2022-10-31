package dk.digitalidentity.sofd.telephony.controller.mvc;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import dk.digitalidentity.sofd.dao.model.enums.PhoneType;
import dk.digitalidentity.sofd.security.RequireTelephonyAccess;
import dk.digitalidentity.sofd.service.FunctionTypeService;
import dk.digitalidentity.sofd.telephony.controller.mvc.dto.TelephonyPhoneDTO;
import dk.digitalidentity.sofd.telephony.controller.service.TelephonyPhoneService;

@RequireTelephonyAccess
@Controller
public class TelephonyController {

	@Autowired
	private TelephonyPhoneService telephonyPhoneService;

	@Autowired
	private FunctionTypeService functionTypeService;
	
	@Autowired
	private MessageSource messageSource;

	@GetMapping("/ui/telephony/list")
	public String list(Model model, Locale loc) {
		model.addAttribute("phones", telephonyPhoneService.findAll().stream().map(p -> new TelephonyPhoneDTO(p)).collect(Collectors.toList()));
		model.addAttribute("functionTypes", functionTypeService.findAllAsDTO());
		
		Map<String, String> map = new HashMap<>();
		
		for (PhoneType phoneType : PhoneType.values()) {
			map.put(phoneType.toString(), messageSource.getMessage(phoneType.getMessage(), null, loc));
		}

		model.addAttribute("phoneTypes", map);

		return "telephony/list";
	}

	@GetMapping("/ui/telephony/history")
	public String history(Model model) {
		return "telephony/history";
	}
}
