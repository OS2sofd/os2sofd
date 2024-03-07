package dk.digitalidentity.sofd.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.sofd.dao.model.AccountOrder;
import dk.digitalidentity.sofd.security.RequireControllerWriteAccess;
import dk.digitalidentity.sofd.service.AccountOrderService;

@RequireControllerWriteAccess
@RestController
public class AccountOrderRestController {

	@Autowired
	private AccountOrderService accountOrderService;

	record DeleteOrder(long orderId) {}
	
	@PostMapping("/rest/accountorder/delete")
	public ResponseEntity<?> delete(@RequestBody DeleteOrder deleteOrder) {
		AccountOrder order = accountOrderService.findById(deleteOrder.orderId);
		if (order != null) {
			accountOrderService.delete(order);
		} else {
			return ResponseEntity.notFound().build();
		}

		return ResponseEntity.ok().build();
	}

}
