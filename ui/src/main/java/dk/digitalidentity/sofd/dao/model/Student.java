package dk.digitalidentity.sofd.dao.model;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import java.util.List;

@Audited
@Getter
@Setter
@Entity
public class Student {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column(nullable = false, length = 36)
	private String uuid;

	@Column(nullable = false)
	private String userId;

	@Column
	private boolean disabled;

	@Column(nullable = false)
	private String name;

	@Column(nullable = true, length = 10)
	private String cpr;

	@NotAudited
	@ElementCollection
	@CollectionTable(name = "student_institution_numbers", joinColumns = @JoinColumn(name = "student_id"))
	@Column(name = "institution_number")
	private List<String> institutionNumbers;

}
