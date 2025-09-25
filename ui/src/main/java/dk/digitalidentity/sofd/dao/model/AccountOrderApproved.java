package dk.digitalidentity.sofd.dao.model;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.hibernate.annotations.CreationTimestamp;

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
