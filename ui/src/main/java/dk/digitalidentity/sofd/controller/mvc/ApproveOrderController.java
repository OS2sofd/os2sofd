package dk.digitalidentity.sofd.controller.mvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import dk.digitalidentity.sofd.config.RoleConstants;
import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.controller.mvc.dto.AccountOrderDTO;
import dk.digitalidentity.sofd.dao.model.AccountOrder;
import dk.digitalidentity.sofd.dao.model.AccountOrderApproved;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.enums.AccountOrderStatus;
import dk.digitalidentity.sofd.security.RequireUserEditOrManager;
import dk.digitalidentity.sofd.security.SecurityUtil;
import dk.digitalidentity.sofd.service.AccountOrderApprovedService;
import dk.digitalidentity.sofd.service.AccountOrderService;
import dk.digitalidentity.sofd.service.PersonService;
import dk.digitalidentity.sofd.service.SupportedUserTypeService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequireUserEditOrManager
public class ApproveOrderController {

	@Autowired
	private AccountOrderService accountOrderService;

	@Autowired
	private SofdConfiguration sofdConfiguration;
	
	@Autowired
	private PersonService personService;
	
	@Autowired
	private AccountOrderApprovedService accountOrderApprovedService;

	@GetMapping("/ui/account/order/approve")
	public String approvePendingOrders(Model model) {
		if (!sofdConfiguration.getModules().getAccountCreation().isAccountOrderApprove()) {
			return "redirect:/";
		}
		
		List<AccountOrder> adAccountOrders = accountOrderService.findByStatusIn(Set.of(AccountOrderStatus.PENDING_APPROVAL)).stream()
				.filter(ao -> SupportedUserTypeService.isActiveDirectory(ao.getUserType()))
				.toList();

		boolean canAccessAllOrders = SecurityUtil.getUserRoles().contains(RoleConstants.USER_ROLE_EDIT);

		Person loggedInPerson = personService.getLoggedInPerson();

		// convert to DTO
		List<AccountOrderDTO> dtos = new ArrayList<>();
		for (AccountOrder order : adAccountOrders) {
			Person person = personService.getByUuid(order.getPersonUuid());
			if (person == null) {
				log.warn("Could not find person with uuid: " + order.getPersonUuid());
				continue;
			}

			if (canAccessAllOrders || canAccess(loggedInPerson, person, order)) {
				dtos.add(new AccountOrderDTO(order, person));
			}
		}

		model.addAttribute("orders", dtos);
		model.addAttribute("customApprovalForm", sofdConfiguration.getModules().getAccountCreation().isCustomApprovalForm());

		return "report/account_orders_approve";
	}

	@PostMapping("/ui/account/order/approve")
	public ResponseEntity<?> approveOrder(@RequestBody AccountOrderDTO body) {
		AccountOrder accountOrder = accountOrderService.findById(body.getId());
		if (accountOrder == null) {
			return ResponseEntity.badRequest().body("Ukendt ordre");
		}

		Person person = personService.getByUuid(accountOrder.getPersonUuid());
		if (person == null) {
			return ResponseEntity.badRequest().body("Ukendt bruger");
		}
		
		boolean canAccessAllOrders = SecurityUtil.getUserRoles().contains(RoleConstants.USER_ROLE_EDIT);
		Person loggedInPerson = personService.getLoggedInPerson();
		if (!canAccessAllOrders && !canAccess(loggedInPerson, person, accountOrder)) {
			return ResponseEntity.badRequest().body("Ingen adgang til kontoordre");
		}

		accountOrder.setStatus(AccountOrderStatus.PENDING);
		accountOrder.setToken(body.getToken());
		accountOrderService.save(accountOrder);

		AccountOrderApproved approval = new AccountOrderApproved();
		approval.setApprovedTts(LocalDateTime.now());
		approval.setApproverName(PersonService.getName(loggedInPerson));
		approval.setApproverUuid(loggedInPerson.getUuid());
		approval.setPersonName(PersonService.getName(person));
		approval.setPersonUuid(person.getUuid());
		approval.setUserId(accountOrder.getRequestedUserId());
		accountOrderApprovedService.save(approval);
		
		return ResponseEntity.ok().build();
	}
	
	@PostMapping("/ui/account/order/reject")
	public ResponseEntity<?> rejectOrder(@RequestBody AccountOrderDTO body) {
		AccountOrder accountOrder = accountOrderService.findById(body.getId());
		if (accountOrder == null) {
			return ResponseEntity.badRequest().body("Ukendt ordre");
		}

		Person person = personService.getByUuid(accountOrder.getPersonUuid());
		if (person == null) {
			return ResponseEntity.badRequest().body("Ukendt bruger");
		}

		boolean canAccessAllOrders = SecurityUtil.getUserRoles().contains(RoleConstants.USER_ROLE_EDIT);
		Person loggedInPerson = personService.getLoggedInPerson();
		if (!canAccessAllOrders && !canAccess(loggedInPerson, person, accountOrder)) {
			return ResponseEntity.badRequest().body("Ingen adgang til kontoordre");
		}
		
		accountOrder.setStatus(AccountOrderStatus.FAILED);
		accountOrder.setMessage("Oprettelse afvist af " + PersonService.getName(loggedInPerson));
		accountOrderService.save(accountOrder);
		
		return ResponseEntity.ok().build();
	}
	
	private boolean canAccess(Person loggedInPerson, Person person, AccountOrder order) {
		// who are the managers of that person
		List<Person> allManagers = new ArrayList<>();

		Person manager = PersonService.getManager(person, order.getEmployeeId());
		if (manager != null) {
			allManagers.add(manager);
		}

		allManagers.addAll(personService.findAllSofdSubstitutesForManager(person, order.getEmployeeId()));

		// check if the currently logged in person is a manager of this user
		if (allManagers.stream().anyMatch(m -> Objects.equals(m.getUuid(), loggedInPerson.getUuid()))) {
			return true;
		}
		
		return false;
	}
}
