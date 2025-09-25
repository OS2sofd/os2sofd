package dk.digitalidentity.sofd.controller.mvc.datatables.dao.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

import dk.digitalidentity.sofd.dao.model.enums.PhoneType;
import dk.digitalidentity.sofd.dao.model.enums.Visibility;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "view_datatables_telephony")
public class GridTelephonyPhone {

	@Id
	@Column
	private long id;
	
	@Column
	private String master;
	
	@Column
	private String phoneNumber;
	
	@Column
	private String vendor;
	
	@Column
	private String accountNumber;
	
	@Column
	private long ean;
	
	@Column
	@Enumerated(EnumType.STRING)
	private PhoneType phoneType;
	
	@Column
	@Enumerated(EnumType.STRING)
	private Visibility visibility;
	
	@Column
	private Long functionTypeId;
	
	@Column
	private String personName;
	
	@Column
	private String personUuid;
	
	@Column
	private String orgunitName;
	
	@Column
	private String orgunitUuid;

	@Column
	private Boolean assigned;
}
