package dk.digitalidentity.sofd.controller.api.v2;

import dk.digitalidentity.sofd.controller.api.v2.model.SchoolUserApiRecord;
import dk.digitalidentity.sofd.controller.api.v2.model.SchoolUserResult;
import dk.digitalidentity.sofd.security.RequireReadAccess;
import dk.digitalidentity.sofd.service.SchoolUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequireReadAccess
public class SchoolUserApi {

	@Autowired
	private SchoolUserService schoolUserService;

	@GetMapping("/api/v2/schoolUsers")
	public ResponseEntity<?> getSchoolEmployeeUsers(@RequestParam(name = "offset", required = false, defaultValue = "-1") long offset, @RequestParam(name = "size", defaultValue = "100") int size) {
		var schoolUsers = schoolUserService.getByOffsetAndLimit(offset, size);

		var result = new SchoolUserResult();
		result.setUsers(new HashSet<>());
		if (!schoolUsers.isEmpty()) {
			result.setNextOffset(schoolUsers.get(schoolUsers.size() - 1).getId());
		}

		result.setUsers(schoolUsers.stream().map(u -> new SchoolUserApiRecord(u)).collect(Collectors.toSet()));
		return new ResponseEntity<>(result, HttpStatus.OK);
	}
}
