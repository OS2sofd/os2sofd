package dk.digitalidentity.sofd.controller.rest.admin.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import dk.digitalidentity.sofd.dao.model.EmailTemplateChild;
import dk.digitalidentity.sofd.dao.model.enums.EmailOrgUnitFilterType;
import dk.digitalidentity.sofd.dao.model.enums.EmployeeFilter;
import dk.digitalidentity.sofd.dao.model.enums.SendTo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EmailTemplateChildDTO {
	private long id;
	private long templateId;
	private String templateName;
	private String title;
	private String message;
	private boolean enabled;
	private long minutesDelay;
	private String recipients;
	private String recipientsCC;
	private String recipientsBCC;
	private SendTo sendTo;
	private EmployeeFilter employeeFilter;
	private List<AttachmentDTO> attachments;
	private long daysBeforeEvent;
	private String domainFilter;
	private List<String> orgUnitUuids = new ArrayList<>();
	private boolean adRequired;
	private boolean onlyManualRecipients;
	// read-only
	private List<String> orgUnitNames = new ArrayList<>();
	private EmailOrgUnitFilterType orgUnitFilterType;
	
	public EmailTemplateChildDTO(EmailTemplateChild child) {
		this.id = child.getId();
		this.templateId = child.getEmailTemplate().getId();
		this.templateName = child.getEmailTemplate().getTemplateType() != null ? child.getEmailTemplate().getTemplateType().getMessage() : "";
		this.title = child.getTitle();
		this.message = child.getMessage();
		this.enabled = child.isEnabled();
		this.minutesDelay = child.getMinutesDelay();
		this.recipients = child.getRecipients();
		this.recipientsCC = child.getRecipientsCC();
		this.recipientsBCC = child.getRecipientsBCC();
		this.sendTo = child.getSendTo();
		this.daysBeforeEvent = child.getDaysBeforeEvent();
		this.domainFilter = child.getDomainFilter();
		this.orgUnitUuids = child.getOrgUnitFilterMappings().stream().map(o -> o.getOrgUnit()).map(o -> o.getUuid()).collect(Collectors.toList());
		this.orgUnitNames = child.getOrgUnitFilterMappings().stream().map(o -> o.getOrgUnit()).map(o -> o.getName()).collect(Collectors.toList());
		this.adRequired = child.isAdRequired();
		this.employeeFilter = child.getEmployeeFilter();
		this.onlyManualRecipients = child.isOnlyManualRecipients();
		this.orgUnitFilterType = child.getOrgUnitFilterType();
	}
}
