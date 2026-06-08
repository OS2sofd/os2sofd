package dk.digitalidentity.sofd.dao.model;

import java.util.Date;
import java.util.Objects;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import dk.digitalidentity.sofd.dao.model.enums.AccountOrderStatus;
import dk.digitalidentity.sofd.dao.model.enums.AccountOrderType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Audited
@Entity(name = "account_orders")
public class AccountOrder {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	// which person/affiliation is the account linked to

	@Column(nullable = false)
	private String personUuid;

	@Column(nullable = true)
	private String employeeId;

	// who ordered the account create/delete and when

	@Column(nullable = true)
	private String requesterUuid;

	@Column(nullable = true)
	private String requesterApiUserId;

	@CreationTimestamp
	@Column(nullable = false)
	private Date orderedTimestamp;

	// details on the actual order

	@Column(nullable = false)
	private String userType;  // SupportedUserType.key

	@Column
	@Enumerated(EnumType.STRING)
	private AccountOrderType orderType;

	@Column(nullable = false)
	private Date activationTimestamp;

	@Column(nullable = true)
	private Date endDate;

	// status on order

	@Column
	@Enumerated(EnumType.STRING)
	private AccountOrderStatus status;

	@Column
	private String requestedUserId;

	@Column
	private String linkedUserId;

	@Column
	private String actualUserId;

	@Column(nullable = true)
	private String message;

	@UpdateTimestamp
	@Column(nullable = false)
	private Date modifiedTimestamp;

	@Column
	private boolean personNotified;

	@Column
	private boolean requesterNotified;

	@Column
	private boolean manual = false;

	@OneToOne
	@JoinColumn(name = "depends_on")
	private AccountOrder dependsOn;
	
	@NotAudited
	@Column
	private String token;

	@ManyToOne
	@JoinColumn(name = "trigger_affiliation_id")
	private Affiliation triggerAffiliation;

	// used for removing duplicate order entries in AccountOrderService
    public boolean logicalEquals(AccountOrder other) {
		return Objects.equals(this.getPersonUuid(), other.getPersonUuid())
				&& Objects.equals(this.getEmployeeId(), other.getEmployeeId())
				&& Objects.equals(this.getUserType(),other.getUserType())
				&& Objects.equals(this.getOrderType(),other.getOrderType())
				&& Objects.equals(this.getStatus(), other.getStatus())
				&& Objects.equals(this.getRequestedUserId(), other.getRequestedUserId());
    }

	public String smallPrint() {
		return "[" + this.getId() + "," + this.getPersonUuid() + "," + this.getUserType() + "," + this.getOrderType() + "," + this.getStatus() + "," + this.getRequestedUserId() + "]";
	}
}
