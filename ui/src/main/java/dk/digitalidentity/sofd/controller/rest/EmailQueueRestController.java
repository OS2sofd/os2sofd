package dk.digitalidentity.sofd.controller.rest;

import java.util.Date;

import dk.digitalidentity.sofd.security.RequireDaoWriteAccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.sofd.dao.model.EmailQueue;
import dk.digitalidentity.sofd.service.EmailQueueService;

@RequireDaoWriteAccess
@RestController
public class EmailQueueRestController {
	
	@Autowired
	private EmailQueueService emailQueueService;

	@PostMapping("/rest/report/emailqueue/expedite/{id}")
	public ResponseEntity<?> expedite(@PathVariable("id") long emailId) {
		EmailQueue email = emailQueueService.findById(emailId);
		if (email == null) {
			return ResponseEntity.notFound().build();
		}
		
		email.setDeliveryTts(new Date());
		emailQueueService.save(email);
		
		return new ResponseEntity<>(HttpStatus.OK);
	}
	
	@PostMapping("/rest/report/emailqueue/cancel/{id}")
	public ResponseEntity<?> cancel(@PathVariable("id") long emailId) {
		EmailQueue email = emailQueueService.findById(emailId);
		if (email == null) {
			return ResponseEntity.notFound().build();
		}
		
		emailQueueService.delete(email);
		
		return new ResponseEntity<>(HttpStatus.OK);
	}
}
