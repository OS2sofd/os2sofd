package dk.digitalidentity.sofd.dao.model;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.BatchSize;

import com.fasterxml.jackson.annotation.JsonBackReference;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity(name = "email_template_child_org_unit")
@Getter
@Setter
@NoArgsConstructor
public class EmailTemplateChildOrgUnitMapping {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@JsonBackReference
	@BatchSize(size = 100)
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "email_template_child_id")
	@NotNull
	private EmailTemplateChild emailTemplateChild;

	@JsonBackReference
	@BatchSize(size = 100)
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "org_unit_uuid")
	@NotNull
	private OrgUnit orgUnit;

	public EmailTemplateChildOrgUnitMapping(EmailTemplateChild emailTemplateChild, OrgUnit orgUnit) {
		this.emailTemplateChild = emailTemplateChild;
		this.orgUnit = orgUnit;
	}
}
