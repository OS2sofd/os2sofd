package dk.digitalidentity.sofd.controller.api;

import java.time.LocalDate;
import java.util.Date;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.controller.api.dto.EmployeeMappingDto;
import dk.digitalidentity.sofd.controller.api.dto.ErrorDTO;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.User;
import dk.digitalidentity.sofd.dao.model.UserChangeEmployeeIdQueue;
import dk.digitalidentity.sofd.security.RequireApiWriteAccess;
import dk.digitalidentity.sofd.security.RequireReadAccess;
import dk.digitalidentity.sofd.service.PersonService;
import dk.digitalidentity.sofd.service.SupportedUserTypeService;
import dk.digitalidentity.sofd.service.UserChangeEmployeeIdQueueService;
import dk.digitalidentity.sofd.service.UserService;
import lombok.extern.slf4j.Slf4j;

@RequireReadAccess
@Slf4j
@RestController
public class ADUserAPIController {

	@Autowired
	private UserService userService;

	@Autowired
	private PersonService personService;

	@Autowired
	private SofdConfiguration configuration;

	@Autowired
	private UserChangeEmployeeIdQueueService userChangeEmployeeIdQueueService;

	@GetMapping("/api/adUser/{userId:.+}/employeeMapping")
	public ResponseEntity<?> getEmployeeMapping(@PathVariable("userId") String userId) {
		User user = userService.findByUserIdAndUserTypeAndMaster(userId, SupportedUserTypeService.getActiveDirectoryUserType(), "ActiveDirectory");
		if (user == null) {
			return ResponseEntity.notFound().build();
		}

		EmployeeMappingDto dto = new EmployeeMappingDto();
		dto.setUserId(user.getUserId());
		dto.setEmployeeId(user.getEmployeeId());
		
		UserChangeEmployeeIdQueue userChangeEntry = userChangeEmployeeIdQueueService.findByUser(user);
		if (userChangeEntry != null) {
			dto.setFutureEmployeeId(userChangeEntry.getEmployeeId());
			dto.setFutureDate(userChangeEntry.getDateOfTransaction());
		}
		return ResponseEntity.ok(dto);
	}

	@RequireApiWriteAccess
	@PostMapping("/api/adUser/{userId:.+}/employeeMapping")
	public ResponseEntity<?> postEmployeeMapping(@RequestBody EmployeeMappingDto body, @PathVariable("userId") String userId) {
		if (body == null) {
			String code = "MissingBody";
			String message = "Request does not contain a body.";
			return new ResponseEntity<>(new ErrorDTO(code, message), HttpStatus.BAD_REQUEST);
		}
		if (userId == null) {
			String code = "MissingUserId";
			String message = "Request url does not contain userId.";
			return new ResponseEntity<>(new ErrorDTO(code, message), HttpStatus.BAD_REQUEST);
		}
		if (!Objects.equals(userId, body.getUserId())) {
			String code = "UserMismatch";
			String message = "The supplied userId does not match the userId in the path.";
			return new ResponseEntity<>(new ErrorDTO(code, message), HttpStatus.BAD_REQUEST);
		}

		User user = userService.findByUserIdAndUserTypeAndMaster(userId, SupportedUserTypeService.getActiveDirectoryUserType(), "ActiveDirectory");
		if (user == null) {
			String code = "UserNotFound";
			String message = "User not found.";
			return new ResponseEntity<>(new ErrorDTO(code, message), HttpStatus.BAD_REQUEST);
		}
		Person person = personService.findByUser(user);
		if (person == null) {
			log.warn("Person not found for user: " + user.getUserId());
			String code = "PersonNotFound";
			String message = "Could not find person in db for provided user.";
			return new ResponseEntity<>(new ErrorDTO(code, message), HttpStatus.BAD_REQUEST);
		}

		if (body.getEmployeeId() != null) {
			boolean hasAffiliation = person.getAffiliations().stream().anyMatch(aff -> 
					Objects.equals(aff.getEmployeeId(), body.getEmployeeId()) &&
					Objects.equals(configuration.getModules().getLos().getPrimeAffiliationMaster(), aff.getMaster()) &&
					(aff.getStopDate() == null || aff.getStopDate().after(new Date())));

			if (!hasAffiliation) {
				String code = "EmploymentNotFound";
				String message = "Could not find employment for provided employmentId";
				return new ResponseEntity<>(new ErrorDTO(code, message), HttpStatus.BAD_REQUEST);
			}
		}

		if ((body.getFutureDate() == null && body.getFutureEmployeeId() != null) ||
				(body.getFutureDate() != null && body.getFutureEmployeeId() == null)) {
			String code = "IncorrectFutureData";
			String message = "Both or neither futureDate and futureEmploymentId must be set.";
			return new ResponseEntity<>(new ErrorDTO(code, message), HttpStatus.BAD_REQUEST);
		}

		if (body.getFutureEmployeeId() != null) {
			boolean hasAffiliation = person.getAffiliations().stream().anyMatch(aff -> 
					Objects.equals(aff.getEmployeeId(), body.getFutureEmployeeId()) &&
					Objects.equals(configuration.getModules().getLos().getPrimeAffiliationMaster(), aff.getMaster()) &&
					(aff.getStopDate() == null || aff.getStopDate().after(new Date())));

			if (!hasAffiliation) {
				String code = "EmploymentNotFoundFuture";
				String message = "Could not found employment for provided futureEmployeeId";
				return new ResponseEntity<>(new ErrorDTO(code, message), HttpStatus.BAD_REQUEST);
			}
		}

		if (body.getFutureDate() != null && !body.getFutureDate().isAfter(LocalDate.now())) {
			String code = "IncorrectDate";
			String message = "The future date has to be after today";
			return new ResponseEntity<>(new ErrorDTO(code, message), HttpStatus.BAD_REQUEST);
		}

		try {
			personService.setEmployeeId(person, user, body.getEmployeeId(), LocalDate.now());
			if (body.getFutureDate() != null && body.getFutureEmployeeId() != null) {
				personService.setEmployeeId(person, user, body.getFutureEmployeeId(), body.getFutureDate());
			}
		} catch (Exception e) {
			String code = "UnknownError";
			String message = "Unknown error occurred: " + e.getMessage();
			return new ResponseEntity<>(new ErrorDTO(code, message), HttpStatus.BAD_REQUEST);
		}

		return ResponseEntity.ok().build();
	}

}
