package dk.digitalidentity.sofd.dao.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;

import lombok.Getter;
import lombok.Setter;

// TODO: this entire concept is dead code, and can be removed at some point in the future (have some queued data in production that needs to be emptied first)
@Getter
@Setter
@Entity(name = "future_emails")
public class FutureEmail {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	
	@Column
	private boolean allOrNone;
	
	@Column
	private Date deliveryTts;
	
	@Column
	private boolean eboks;
	
	@Column
	private String title;
	
	@Column
	private String message;
	
	@ManyToMany
	@JoinTable(name = "future_emails_persons", joinColumns = @JoinColumn(name = "future_email_id"), inverseJoinColumns = @JoinColumn(name = "person_uuid"))
	private List<Person> persons;
	
	public FutureEmail() {
		this.persons = new ArrayList<>();
	}	
}
