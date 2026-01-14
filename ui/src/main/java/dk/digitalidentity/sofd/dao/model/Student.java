package dk.digitalidentity.sofd.dao.model;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
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
