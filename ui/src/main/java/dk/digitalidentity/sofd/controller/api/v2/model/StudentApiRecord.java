package dk.digitalidentity.sofd.controller.api.v2.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import dk.digitalidentity.sofd.dao.model.Student;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(exclude = { "cpr" })
@NoArgsConstructor
public class StudentApiRecord extends BaseRecord {

	// primary key
	@NotNull
	private String username;

	// read/write fields
	@Size(min = 10, max = 10)
	@Pattern(regexp = "([0-9]{10})", message = "Invalid cpr")
	private String cpr;

	@Size(max = 255)
	@NotNull
	private String name;

	private Boolean disabled;
	private List<String> institutionNumbers;

	// readonly
	private String uuid;
	private long id;

	public StudentApiRecord(Student student) {
		this.id = student.getId();
		this.cpr = student.getCpr();
		this.uuid = student.getUuid();
		this.username = student.getUserId();
		this.name = student.getName();
		this.disabled = student.isDisabled();
		this.institutionNumbers = student.getInstitutionNumbers();
	}

	public Student toStudent(Student actualStudent) {
		Student student = new Student();

		if (actualStudent == null) {
			student.setUuid(UUID.randomUUID().toString());
		}
		else {
			student.setUuid(actualStudent.getUuid());
		}

		student.setCpr(cpr);
		student.setUserId(username);
		student.setName(name);
		student.setDisabled(disabled != null ? disabled : false);

		if (institutionNumbers != null) {
			student.setInstitutionNumbers(new ArrayList<>());
			student.getInstitutionNumbers().addAll(institutionNumbers);
		}

		return student;
	}
}
