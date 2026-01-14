package dk.digitalidentity.sofd.dao.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity(name = "account_orders_approved")
public class AccountOrderApproved {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column
	private String approverUuid;
	
	@Column
	private String approverName;

	@Column
	private String personUuid;
	
	@Column
	private String personName;

	@Column
	private String userId;
	
	@CreationTimestamp
	@Column
	private LocalDateTime approvedTts;

}
