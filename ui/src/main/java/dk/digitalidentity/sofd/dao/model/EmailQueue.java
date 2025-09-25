package dk.digitalidentity.sofd.dao.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "email_queue")
public class EmailQueue {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column
	@NotNull
	private String title;

	@Column
	@NotNull
	private String message;
	
	@Column
	private String cpr;
	
	@Column
	private String email;
	
	@Column
	private String personUuid;
	
	@Column
	private boolean performEmailCheck;
	
	@Column
	@Temporal(TemporalType.TIMESTAMP)
	private Date deliveryTts;
	
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "email_template_child_id")
	private EmailTemplateChild emailTemplateChild;

	@Column
	private String recipient;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "trigger_account_order_id")
	private AccountOrder triggerAccountOrder;

	@Column
	private String logContext;
}
