package dk.digitalidentity.sofd.controller.api;

import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.sofd.dao.model.BadWord;
import dk.digitalidentity.sofd.security.RequireReadAccess;
import dk.digitalidentity.sofd.service.BadWordsService;

@RequireReadAccess
@RestController
public class BadWordsApiController {

	@Autowired
	private BadWordsService badWordsService;

	@GetMapping("/api/badwords")
	public ResponseEntity<?> list() {
		return new ResponseEntity<>(badWordsService.findAll().stream()
				.map(BadWord::getValue)
				.collect(Collectors.toList()),
				HttpStatus.OK);
	}
}
