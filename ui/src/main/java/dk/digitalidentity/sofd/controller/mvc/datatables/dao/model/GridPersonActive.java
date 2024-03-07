package dk.digitalidentity.sofd.controller.mvc.datatables.dao.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "view_datatables_persons")
public class GridPersonActive implements GridPerson {

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
	
	@Column
	private boolean forceStop;
	
	@Column
	private boolean disableAccountOrders;
	
	@Column
	private boolean dead;
	
	@Column
	private boolean disenfranchised;

	@Column
	private String userIds;

	@Column
	private boolean fictiveCpr;
}
