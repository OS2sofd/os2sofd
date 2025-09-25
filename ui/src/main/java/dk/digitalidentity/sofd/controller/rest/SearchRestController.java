package dk.digitalidentity.sofd.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.sofd.security.RequireReadAccess;
import dk.digitalidentity.sofd.service.SearchService;

@RequireReadAccess
@RestController
public class SearchRestController {

	@Autowired
	private SearchService searchService;

	@GetMapping("/rest/search/{term}")
	public ResponseEntity<?> getLocalExtensions(@PathVariable("term") String term) {
		return new ResponseEntity<>(searchService.search(term), HttpStatus.OK);
	}
}
