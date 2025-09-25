package dk.digitalidentity.sofd.controller.api.v2;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.controller.api.v2.model.InstitutionApiRecord;
import dk.digitalidentity.sofd.controller.api.v2.model.StudentApiRecord;
import dk.digitalidentity.sofd.controller.api.v2.model.StudentResult;
import dk.digitalidentity.sofd.dao.model.Institution;
import dk.digitalidentity.sofd.dao.model.Student;
import dk.digitalidentity.sofd.security.RequireApiWriteAccess;
import dk.digitalidentity.sofd.security.RequireReadAccess;
import dk.digitalidentity.sofd.security.SecurityUtil;
import dk.digitalidentity.sofd.service.InstitutionService;
import dk.digitalidentity.sofd.service.StudentService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequireReadAccess
public class StudentApi {
	
	@Autowired
	private StudentService studentService;

	@Autowired
	private InstitutionService institutionService;

	@Autowired
	private SofdConfiguration sofdConfiguration;

	@GetMapping("/api/v2/students")
	public ResponseEntity<?> getStudents(@RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "offset", required = false, defaultValue = "-1") long offset, @RequestParam(name = "size", defaultValue = "100") int size) {
		if (!sofdConfiguration.getModules().getStudents().isEnabled()) {
			return new ResponseEntity<>("The student module is disabled in SOFD", HttpStatus.BAD_REQUEST);
		}

		List<Student> students = null;
		
		if (offset < 0 && page > 0) {
			log.warn("/api/v2/students called with page - this is bad performance and the caller should switch to offset");
			students = studentService.getAll(PageRequest.of(page, size, Sort.by("id"))).getContent();
		}
		else {
			students = studentService.getByOffsetAndLimit(offset, size);
		}
		
		StudentResult result = new StudentResult();
		result.setStudents(new HashSet<>());
		result.setPage(page);
		if (students.size() > 0) {
			result.setNextOffset(students.get(students.size() - 1).getId());
		}

		for (Student student : students) {
			result.getStudents().add(new StudentApiRecord(student));
		}

		return new ResponseEntity<>(result, HttpStatus.OK);
	}

	@GetMapping("/api/v2/students/institutions")
	public ResponseEntity<?> getInstitutions() {
		if (!sofdConfiguration.getModules().getStudents().isEnabled()) {
			return new ResponseEntity<>("The student module is disabled in SOFD", HttpStatus.BAD_REQUEST);
		}

		List<Institution> institutions = institutionService.getAll();
		List<InstitutionApiRecord> result = institutions.stream().map(i -> new InstitutionApiRecord(i)).collect(Collectors.toList());

		return new ResponseEntity<>(result, HttpStatus.OK);
	}

	// cpr is optional, so this might not return a student
	@GetMapping("/api/v2/students/byCpr/{cpr}")
	public ResponseEntity<?> getStudentByCpr(@PathVariable("cpr") String cpr) {
		if (!sofdConfiguration.getModules().getStudents().isEnabled()) {
			return new ResponseEntity<>("The student module is disabled in SOFD", HttpStatus.BAD_REQUEST);
		}

		Student student = studentService.findByCpr(cpr);
		if (student == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		return new ResponseEntity<>(new StudentApiRecord(student), HttpStatus.OK);
	}

	@GetMapping("/api/v2/students/byADUserId/{userId}")
	public ResponseEntity<?> getStudentByADUserId(@PathVariable("userId") String userId) {
		if (!sofdConfiguration.getModules().getStudents().isEnabled()) {
			return new ResponseEntity<>("The student module is disabled in SOFD", HttpStatus.BAD_REQUEST);
		}

		Student student = studentService.findByUserId(userId);
		if (student == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		
		return new ResponseEntity<>(new StudentApiRecord(student), HttpStatus.OK);
	}

	@GetMapping("/api/v2/students/{uuid}")
	public ResponseEntity<?> getStudent(@PathVariable("uuid") String uuid) {
		if (!sofdConfiguration.getModules().getStudents().isEnabled()) {
			return new ResponseEntity<>("The student module is disabled in SOFD", HttpStatus.BAD_REQUEST);
		}

		Student student = studentService.getByUuid(uuid);
		if (student == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		
		return new ResponseEntity<>(new StudentApiRecord(student), HttpStatus.OK);
	}

	@RequireApiWriteAccess
	@PostMapping("/api/v2/students")
	public ResponseEntity<?> createStudent(@Valid @RequestBody StudentApiRecord studentRecord, BindingResult bindingResult) {
		if (!sofdConfiguration.getModules().getStudents().isEnabled()) {
			return new ResponseEntity<>("The student module is disabled in SOFD", HttpStatus.BAD_REQUEST);
		}

		if (bindingResult.hasErrors()) {
			return new ResponseEntity<>(bindingResult.getAllErrors(), HttpStatus.BAD_REQUEST);
		}

		if (studentService.findByUserId(studentRecord.getUsername()) != null) {
			log.warn("student with userId " + studentRecord.getUsername() + " already exists");
			return new ResponseEntity<>("Already exists", HttpStatus.CONFLICT);
		}

		List<String> institutionNumbers = institutionService.getAlInstitutionNumbers();
		for (String institutionNumber : studentRecord.getInstitutionNumbers()) {
			if (!institutionNumbers.contains(institutionNumber)) {
				log.warn("Student " + studentRecord.getUsername() + " has an institution that does not exist " + institutionNumber);
				return new ResponseEntity<>("The student has one or more institutions not supported in SOFD", HttpStatus.BAD_REQUEST);
			}
		}

		Student student = studentService.save(studentRecord.toStudent(null));
		
		return new ResponseEntity<>(new StudentApiRecord(student), HttpStatus.CREATED);
	}

	@DeleteMapping("/api/v2/students/deleteByUsername/{userId}")
	public ResponseEntity<?> deleteStudentByUsername(@PathVariable("userId") String userId) {
		if (!sofdConfiguration.getModules().getStudents().isEnabled()) {
			return new ResponseEntity<>("The student module is disabled in SOFD", HttpStatus.BAD_REQUEST);
		}

		Student student = studentService.findByUserId(userId);
		if (student == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		studentService.delete(student);

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequireApiWriteAccess
	@PatchMapping("/api/v2/students/{userId}")
	public ResponseEntity<?> patchStudent(@PathVariable("userId") String userId, @RequestBody StudentApiRecord studentRecord, BindingResult bindingResult) throws Exception {
		if (!sofdConfiguration.getModules().getStudents().isEnabled()) {
			return new ResponseEntity<>("The student module is disabled in SOFD", HttpStatus.BAD_REQUEST);
		}

		try {
			Student student = studentService.findByUserId(userId);
			if (student == null) {
				return new ResponseEntity<>(HttpStatus.NOT_FOUND);
			}

			if (studentRecord.getInstitutionNumbers() != null) {
				List<String> institutionNumbers = institutionService.getAlInstitutionNumbers();
				for (String institutionNumber : studentRecord.getInstitutionNumbers()) {
					if (!institutionNumbers.contains(institutionNumber)) {
						log.warn("Student " + studentRecord.getUsername() + " has an institution that does not exist " + institutionNumber);
						return new ResponseEntity<>("The student has one or more institutions not supported in SOFD", HttpStatus.BAD_REQUEST);
					}
				}
			}
			
			boolean changes = patch(student, studentRecord);

			if (changes) {
				student = studentService.save(student);
			}
			
			if (!changes) {
				return new ResponseEntity<>(HttpStatus.NOT_MODIFIED);
			}

			return new ResponseEntity<>(new StudentApiRecord(student), HttpStatus.OK);
		}
		catch (Exception ex) {
			log.error("Failed to patch " + userId + " with payload from client " + ((SecurityUtil.getClient() != null) ? SecurityUtil.getClient().getId() : "-1") + " - payload = " + studentRecord.toString());
			// let Spring map the exception to a HTTP 500
			throw ex;
		}
	}

	private boolean patch(Student student, StudentApiRecord studentRecord) throws Exception {
		Student studentFromRecord = studentRecord.toStudent(student);
		boolean changes = false;
		
		// in patch() fields are only updated if the supplied studentFromRecord is non-null, meaning PATCH cannot
		// null a field - a PUT operation must be implemented for null'ing to be possible.

		if (studentFromRecord.getName() != null && !Objects.equals(studentFromRecord.getName(), student.getName())) {
			student.setName(studentFromRecord.getName());
			changes = true;
		}

		if (studentRecord.getDisabled() != null && studentFromRecord.isDisabled() != student.isDisabled()) {
			student.setDisabled(studentFromRecord.isDisabled());
			changes = true;
		}

		if (studentFromRecord.getCpr() != null && !Objects.equals(studentFromRecord.getCpr(), student.getCpr())) {
			student.setCpr(studentFromRecord.getCpr());
			changes = true;
		}
		
		// due to the way patching works, it is not possible "null" a collection using the PATCH operation,
		// an empty collection must be supplied to "empty" it.
		if (studentFromRecord.getInstitutionNumbers() != null) {
			changes = patchInstitutionNumbers(student, studentFromRecord, changes);
		}

		return changes;
	}

	private static boolean patchInstitutionNumbers(Student student, Student record, boolean changes) {
		if (student.getInstitutionNumbers() == null) {
			student.setInstitutionNumbers(new ArrayList<>());
			changes = true;
		}

		for (String institutionNumber : record.getInstitutionNumbers()) {
			// add to collection if not contains
			if (!student.getInstitutionNumbers().contains(institutionNumber)) {
				student.getInstitutionNumbers().add(institutionNumber);
				changes = true;
			}
		}

		// remove if not in supplied list
		List<String> toRemove = student.getInstitutionNumbers().stream().filter(i -> !record.getInstitutionNumbers().contains(i)).collect(Collectors.toList());
		if (!toRemove.isEmpty()) {
			student.getInstitutionNumbers().removeIf(toRemove::contains);
			changes = true;
		}

		return changes;
	}
}
