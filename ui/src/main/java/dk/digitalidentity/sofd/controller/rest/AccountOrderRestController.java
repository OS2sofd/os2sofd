package dk.digitalidentity.sofd.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.sofd.dao.model.AccountOrder;
import dk.digitalidentity.sofd.dao.model.enums.EntityType;
import dk.digitalidentity.sofd.dao.model.enums.EventType;
import dk.digitalidentity.sofd.log.AuditLogger;
import dk.digitalidentity.sofd.security.RequireControllerWriteAccess;
import dk.digitalidentity.sofd.service.AccountOrderService;
import dk.digitalidentity.sofd.service.PersonService;

@RequireControllerWriteAccess
@RestController
public class AccountOrderRestController {

	@Autowired
	private AccountOrderService accountOrderService;
	
	@Autowired
	private AuditLogger auditLogger;
	
	@Autowired
	private PersonService personService;

	record DeleteOrder(long orderId) {}
	
	@PostMapping("/rest/accountorder/delete")
	public ResponseEntity<?> delete(@RequestBody DeleteOrder deleteOrder) {
		AccountOrder order = accountOrderService.findById(deleteOrder.orderId);
		if (order != null) {
			accountOrderService.delete(order);
		} else {
			return ResponseEntity.notFound().build();
		}
		
		String message = "Kontoordre slettet: Handling: " + order.getOrderType() + " Type: " + order.getUserType() + " Status: " + order.getStatus() + ".";
		auditLogger.log(order.getPersonUuid(), EntityType.ACCOUNT_ORDER, EventType.DELETE, PersonService.getName(personService.getByUuid(order.getPersonUuid())), message);

		return ResponseEntity.ok().build();
	}

}
