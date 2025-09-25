package dk.digitalidentity.sofd.controller.rest.admin;

import dk.digitalidentity.sofd.dao.model.Institution;
import dk.digitalidentity.sofd.security.RequireAdminAccess;
import dk.digitalidentity.sofd.service.InstitutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RequireAdminAccess
@RestController
public class InstitutionRestController {

	@Autowired
	private InstitutionService institutionService;

	record InstitutionDTO(String name, String number) {}

	@PostMapping("/rest/institution/save")
	@ResponseBody
	public ResponseEntity<String> saveInstitution(@RequestBody InstitutionDTO institutionDTO) {
		if (!StringUtils.hasLength(institutionDTO.name()) || !StringUtils.hasLength(institutionDTO.number())) {
			return new ResponseEntity<>("Udfyld både navn og institutionsnummer", HttpStatus.BAD_REQUEST);
		}

		Institution institution = new Institution();
		institution.setUuid(UUID.randomUUID().toString());
		institution.setInstitutionNumber(institutionDTO.number().trim());
		institution.setName(institutionDTO.name().trim());
		institutionService.save(institution);

		return new ResponseEntity<>(HttpStatus.OK);
	}
}
