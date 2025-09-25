package dk.digitalidentity.sofd.controller.mvc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import dk.digitalidentity.sofd.controller.mvc.dto.EmailQueueDTO;
import dk.digitalidentity.sofd.dao.model.EmailQueue;
import dk.digitalidentity.sofd.security.RequireReadAccess;
import dk.digitalidentity.sofd.service.EmailQueueService;

@RequireReadAccess
@Controller
public class EmailQueueController {

	@Autowired
	private EmailQueueService emailQueueService;

	@GetMapping("/ui/report/emailqueue")
	public String accountOrders(Model model) {
		List<EmailQueue> pendingEmails = emailQueueService.findAll();
		pendingEmails.sort(Comparator.comparing(EmailQueue::getDeliveryTts));

		List<EmailQueueDTO> dtos = new ArrayList<>();
		for (EmailQueue pendingEmail : pendingEmails) {
			dtos.add(new EmailQueueDTO(pendingEmail));
		}

		model.addAttribute("emails", dtos);

		return "report/emailqueue";
	}
}
