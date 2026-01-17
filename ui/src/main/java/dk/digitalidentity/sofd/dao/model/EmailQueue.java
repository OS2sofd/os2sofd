package dk.digitalidentity.sofd.dao.model;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
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
