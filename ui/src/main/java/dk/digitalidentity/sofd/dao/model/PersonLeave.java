package dk.digitalidentity.sofd.dao.model;

import java.util.Date;

import org.hibernate.envers.Audited;

import dk.digitalidentity.sofd.dao.model.enums.LeaveReason;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity(name = "persons_leave")
@Getter
@Setter
@Audited
public class PersonLeave {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column
	private Date startDate;

	@Column
	private Date stopDate;

	@Column
	@Enumerated(EnumType.STRING)
	private LeaveReason reason;

	@Column
	private String reasonText;

	@Column
	private boolean disableAccountOrders;

	@Column
	private boolean expireAccounts;

}
