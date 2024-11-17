package dk.digitalidentity.sofd.dao.model;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import dk.digitalidentity.sofd.dao.model.enums.EmailOrgUnitFilterType;
import org.hibernate.annotations.BatchSize;

import com.fasterxml.jackson.annotation.JsonBackReference;

import dk.digitalidentity.sofd.dao.model.enums.EmployeeFilter;
import dk.digitalidentity.sofd.dao.model.enums.SendTo;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "email_template_children")
public class EmailTemplateChild {

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
	private boolean enabled;
	
	@Column
	private long minutesDelay;
	
	@Column
	private String recipients;

	@Column(name = "recipients_cc")
	private String recipientsCC;

	@Column(name = "recipients_bcc")
	private String recipientsBCC;

	@Enumerated(EnumType.STRING)
	@Column
	private SendTo sendTo;

	@Enumerated(EnumType.STRING)
	@Column
	private EmployeeFilter employeeFilter;
	
	@Column
	private long daysBeforeEvent;

	@Column
	private String domainFilter;
	
	@Column
	private boolean adRequired;

	@Column
	private boolean onlyManualRecipients;

	@Enumerated(EnumType.STRING)
	@Column
	private EmailOrgUnitFilterType orgUnitFilterType;

	@JsonBackReference
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "email_template_id")
	private EmailTemplate emailTemplate;
	
	@BatchSize(size = 50)
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "emailTemplateChild")
	private List<EmailTemplateChildOrgUnitMapping> orgUnitFilterMappings;
	
	@OneToMany(mappedBy = "emailTemplateChild", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	private List<Attachment> attachments;

	@Column
	private boolean rawTemplate;

	// when sending emails, it is done Async, so on another thread, with no session open
	public void forceLoadAttachments() {
		if (attachments != null) {
			for (Attachment attachment : attachments) {
				
				@SuppressWarnings("unused")
				int length = attachment.getFile().getContent().length;
			}
		}		
	}
}
