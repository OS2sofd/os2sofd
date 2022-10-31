package dk.digitalidentity.sofd.controller.api;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.sofd.controller.api.dto.AccountOrderDTO;
import dk.digitalidentity.sofd.controller.api.dto.AccountOrderResponseDTO;
import dk.digitalidentity.sofd.controller.api.dto.CreateAccountOrderDTO;
import dk.digitalidentity.sofd.controller.api.dto.SetOrderStatusDTO;
import dk.digitalidentity.sofd.dao.model.AccountOrder;
import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.Notification;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.SupportedUserType;
import dk.digitalidentity.sofd.dao.model.enums.AccountOrderStatus;
import dk.digitalidentity.sofd.dao.model.enums.AccountOrderType;
import dk.digitalidentity.sofd.dao.model.enums.EndDate;
import dk.digitalidentity.sofd.dao.model.enums.EntityType;
import dk.digitalidentity.sofd.dao.model.enums.NotificationType;
import dk.digitalidentity.sofd.security.RequireApiWriteAccess;
import dk.digitalidentity.sofd.service.AccountOrderService;
import dk.digitalidentity.sofd.service.AffiliationService;
import dk.digitalidentity.sofd.service.NotificationService;
import dk.digitalidentity.sofd.service.PersonService;
import dk.digitalidentity.sofd.service.SupportedUserTypeService;
import dk.digitalidentity.sofd.service.UsernameGeneratorService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequireApiWriteAccess
public class AccountOrderApiController {

	@Autowired
	private AccountOrderService accountOrderService;

	@Autowired
	private PersonService personService;

	@Autowired
	private NotificationService notificationService;

	@Autowired
	private SupportedUserTypeService supportedUserTypeService;

	@Autowired
	private UsernameGeneratorService usernameGeneratorService;

	@PostMapping("/api/account/fullSync")
	public ResponseEntity<?> triggerFullSync() {
		log.info("Full nightly update triggered through API");

		accountOrderService.nightlyJob();

		return ResponseEntity.ok().build();
	}

	/**
	 * Creates an account order
	 */
	@PostMapping("/api/account")
	public ResponseEntity<?> createAccountOrder(@RequestBody CreateAccountOrderDTO order) {
		Person person = personService.getByUuid(order.getPersonUuid());
		if (person == null) {
			log.warn("Person does not exist: " + order.getPersonUuid());
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		SupportedUserType supportedUserType = supportedUserTypeService.findByKey(order.getUserType());
		if (supportedUserType == null) {
			log.warn("UserType does not exist: " + order.getUserType());
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		// if an affiliationUuid is supplied, scan for it
		String employeeId = null;
		for (Affiliation affiliation : AffiliationService.onlyActiveAffiliations(person.getAffiliations())) {
			if (affiliation.getUuid().equals(order.getAffiliationUuid())) {
				employeeId = affiliation.getEmployeeId();
				break;
			}
		}

		if (StringUtils.hasLength(order.getChosenUserId())) {
			order.setChosenUserId(order.getChosenUserId().trim());
		}
		else {
			String generatedUserId = usernameGeneratorService.getUsername(person, employeeId, order.getUserType(), null);
			order.setChosenUserId(generatedUserId == null ? "" : generatedUserId);
		}

		// extra validation for exchange accounts
		if (SupportedUserTypeService.isExchange(order.getUserType())) {
			Set<String> userIds = accountOrderService.getActiveDirectoryUsersForExchangeAccount(person.getAffiliations(), false);

			// if they have added an actual mail domain, trim it
			if (order.getChosenUserId().contains("@")) {
				order.setChosenUserId(order.getChosenUserId().substring(0, order.getChosenUserId().indexOf("@")));
			}

			if (!userIds.contains(order.getUserId())) {
				log.warn("Chosen userId is not valid for ordering an Exchange Account: " + order.getUserId());
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}
		}

		AccountOrder accountOrder = accountOrderService.createAccountOrder(
				person,
				supportedUserType,
				order.getUserId(),
				employeeId,
				new Date(),
				(SupportedUserTypeService.getActiveDirectoryUserType().equals(supportedUserType.getKey()) ? order.getUserEndDate() : EndDate.NO));

		accountOrder.setRequestedUserId(order.getChosenUserId());

		AccountOrder result = accountOrderService.save(accountOrder);
		return new ResponseEntity<AccountOrder>(result, HttpStatus.OK);
	}

	/**
	 * Returns all orders of a specific UserType,
	 */
	@GetMapping("/api/account/{type}/all")
	public ResponseEntity<List<AccountOrderDTO>> getAllOrders(@PathVariable("type") String userType) {
		SupportedUserType supportedUserType = supportedUserTypeService.findByKey(userType);
		if (supportedUserType == null) {
			log.error("Unknown userType on getPendingOrders: " + userType);
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		List<AccountOrder> allOrders = accountOrderService.getOrdersByUserType(userType);

		List<AccountOrderDTO> result = new ArrayList<AccountOrderDTO>();
		for (AccountOrder order : allOrders) {
			Person person = personService.getByUuid(order.getPersonUuid());
			if (person != null) {
				AccountOrderDTO dto = new AccountOrderDTO(order, person, 0);
				result.add(dto);
			}
		}
		return new ResponseEntity<List<AccountOrderDTO>>(result, HttpStatus.OK);
	}

	/**
	 * Returns all pending orders (CREATE, DEACTIVATE and DELETE) of a specific UserType,
	 * filtered by ActivationTimestamp, so only those that are in need of being processed
	 * now will be returned
	 */
	@GetMapping("/api/account/{type}/pending")
	public ResponseEntity<AccountOrderResponseDTO> getPendingOrders(@PathVariable("type") String userType, @RequestParam("type") AccountOrderType type) {
		SupportedUserType supportedUserType = supportedUserTypeService.findByKey(userType);
		if (supportedUserType == null) {
			log.error("Unknown userType on getPendingOrders: " + userType);
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		AccountOrderResponseDTO responseDTO = new AccountOrderResponseDTO();
		responseDTO.setSingleAccount(supportedUserType.isSingleUserMode());
		long userIdLength = 0;

		List<AccountOrder> pendingOrders = accountOrderService.getPendingOrders(userType, type);

		pendingOrders = accountOrderService.identifyAndDeleteDuplicates(pendingOrders);

		responseDTO.setPendingOrders(new ArrayList<AccountOrderDTO>());
		for (AccountOrder order : pendingOrders) {
			Person person = personService.getByUuid(order.getPersonUuid());
			if (person != null) {
				AccountOrderDTO dto = new AccountOrderDTO(order, person, userIdLength);

				responseDTO.getPendingOrders().add(dto);
			}
		}

		return new ResponseEntity<AccountOrderResponseDTO>(responseDTO, HttpStatus.OK);
	}

	/**
	 * update status on orders processed by external agents
	 */
	@PostMapping("/api/account/{type}/setStatus")
	public ResponseEntity<String> setOrderStatus(@PathVariable("type") String userType, @RequestBody List<SetOrderStatusDTO> request) {

		for (SetOrderStatusDTO dto : request) {
			AccountOrder accountOrder = accountOrderService.findById(dto.getId());
			if (accountOrder == null) {
				log.error("Unknown AccountOrder: " + dto.getId());
				continue;
			}

			if (!accountOrder.getUserType().equals(userType)) {
				log.error("UserType mismatch: " + accountOrder.getUserType() + " != " + userType + " for: " + accountOrder.getId());
				continue;
			}

			if (!accountOrder.getStatus().equals(AccountOrderStatus.PENDING)) {
				log.error("AccountOrder status mismatch, expected PENDING, was " + accountOrder.getStatus() + " for: " + accountOrder.getId());
				continue;
			}

			accountOrder.setStatus(dto.getStatus());
			accountOrder.setMessage(dto.getMessage());

			if (dto.getStatus().equals(AccountOrderStatus.FAILED)) {
				Person person = personService.getByUuid(accountOrder.getPersonUuid());
				if (person != null) {
					String prettyName = supportedUserTypeService.getPrettyName(accountOrder.getUserType());

					Notification notification = new Notification();
					notification.setActive(true);
					notification.setAffectedEntityName(PersonService.getName(person));
					notification.setAffectedEntityType(EntityType.PERSON);
					notification.setAffectedEntityUuid(accountOrder.getPersonUuid());
					notification.setMessage(prettyName + ": " + dto.getMessage());
					notification.setNotificationType(NotificationType.ACCOUNT_ORDER_FAILURE);
					notification.setCreated(new Date());

					notificationService.save(notification);
				}
			}
			else {
				accountOrder.setActualUserId(dto.getAffectedUserId());

				if ((dto.getStatus().equals(AccountOrderStatus.CREATED) || dto.getStatus().equals(AccountOrderStatus.REACTIVATED)) &&
						SupportedUserTypeService.isActiveDirectory(userType)) {

					AccountOrder orderDependingOn = accountOrderService.findByDependsOn(accountOrder);
					if (orderDependingOn != null) {
						orderDependingOn.setStatus(AccountOrderStatus.PENDING);
						orderDependingOn.setLinkedUserId(accountOrder.getActualUserId());

						// if there is a delay configured (there very like is for Exchange), then we need to add that to ensure it
						// does not create the account immediately
						SupportedUserType orderDependingOnType = supportedUserTypeService.findByKey(orderDependingOn.getUserType());
						if (orderDependingOnType != null && orderDependingOnType.getMinutesDelay() > 0) {
							Calendar cal = Calendar.getInstance();
							cal.setTime(new Date());
							cal.add(Calendar.MINUTE, (int) orderDependingOnType.getMinutesDelay());

							orderDependingOn.setActivationTimestamp(cal.getTime());
						}

						accountOrderService.save(orderDependingOn);
					}
				}
			}

			try {
				accountOrderService.notify(accountOrder);
			}
			catch (Exception ex) {
				log.error("Failed to notify on order: " + accountOrder.getId(), ex);
			}

			accountOrderService.save(accountOrder);
		}

		return new ResponseEntity<String>(HttpStatus.OK);
	}
}
