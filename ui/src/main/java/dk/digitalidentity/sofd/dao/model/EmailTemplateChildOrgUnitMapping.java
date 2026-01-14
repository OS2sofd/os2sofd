package dk.digitalidentity.sofd.dao.model;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;
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
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "email_template_child_id")
	@NotNull
	private EmailTemplateChild emailTemplateChild;

	@JsonBackReference
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "org_unit_uuid")
	@NotNull
	private OrgUnit orgUnit;

	public EmailTemplateChildOrgUnitMapping(EmailTemplateChild emailTemplateChild, OrgUnit orgUnit) {
		this.emailTemplateChild = emailTemplateChild;
		this.orgUnit = orgUnit;
	}
}
