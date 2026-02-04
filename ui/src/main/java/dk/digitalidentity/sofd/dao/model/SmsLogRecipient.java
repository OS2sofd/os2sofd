package dk.digitalidentity.sofd.dao.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import dk.digitalidentity.sofd.controller.mvc.dto.RecipientType;
import lombok.Getter;
import lombok.Setter;

@Entity(name = "sms_log_recipients")
@Getter
@Setter
public class SmsLogRecipient {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column
	private String name;

	@Column
	private String phone;

	@Column
	private String email;

	@Column
	@Enumerated(EnumType.STRING)
	private RecipientType type;

	@ManyToOne
	@JoinColumn(name = "sms_log_id", nullable = false)
	private SmsLog smsLog;
}