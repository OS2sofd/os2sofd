package dk.digitalidentity.sofd.dao.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import dk.digitalidentity.sofd.dao.model.enums.LeaveReason;
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
	@Temporal(TemporalType.DATE)
	private Date startDate;

	@Column
	@Temporal(TemporalType.DATE)
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
