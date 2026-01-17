package dk.digitalidentity.sofd.dao.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

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