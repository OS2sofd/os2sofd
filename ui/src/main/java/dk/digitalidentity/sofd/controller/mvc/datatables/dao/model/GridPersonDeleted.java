package dk.digitalidentity.sofd.controller.mvc.datatables.dao.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonIgnore;

import dk.digitalidentity.sofd.dao.model.enums.LeaveReason;
import dk.digitalidentity.sofd.dao.model.enums.PersonType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "view_datatables_persons_deleted")
public class GridPersonDeleted implements GridPerson {

	@Id
	@Column
	private String uuid;

	@Column
	private String name;

	@Column
	private String affiliation;
	
	@Column
	private String orgunitUuid;

	@Column
	private String phoneNumber;

	// JsonIgnore prevents leaking cpr back to ui
	@JsonIgnore
	@Column
	private String cpr;
	
	@Column
	private boolean leave;

	@Enumerated(EnumType.STRING)
	@Column
	private LeaveReason reason;

	@Transient
	private String reasonTranslated;

	@Column
	private String reasonText;

	@Column
	private boolean forceStop;

	@Column
	private boolean disableAccountOrdersCreate;

	@Column
	private boolean disableAccountOrdersDisable;

	@Column
	private boolean disableAccountOrdersDelete;
	
	@Column
	private boolean dead;
	
	@Column
	private boolean disenfranchised;

	@Column
	private String userIds;
	
	@Column
	private boolean fictiveCpr;

	@Column
	@Enumerated(EnumType.STRING)
	private PersonType personType;
}
