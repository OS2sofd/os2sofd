package dk.digitalidentity.sofd.telephony.dao.model;

import java.util.Date;
import java.util.List;

import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.envers.Audited;

import dk.digitalidentity.sofd.dao.model.FunctionType;
import dk.digitalidentity.sofd.dao.model.enums.PhoneType;
import dk.digitalidentity.sofd.dao.model.enums.Visibility;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Entity(name = "telephony_phones")
@Getter
@Setter
@Audited
public class TelephonyPhone {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column
	private String master;
	
	@Column
	private String masterId;

	@Column
	@NotNull
	@Size(max = 128)
	private String phoneNumber;

	@Column
	@Size(max = 128)
	private String vendor;

	@Column
	@Size(max = 128)
	private String accountNumber;

	@Column
	private long ean;

	@Column
	@Enumerated(EnumType.STRING)
	private PhoneType phoneType;

	@Column
	@Enumerated(EnumType.STRING)
	private Visibility visibility;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "function_type_id")
	private FunctionType functionType;

	@UpdateTimestamp
	@Column
	private Date lastChanged;
	
	@Column
	private String personUuid;
	
	@Column
	private String personName;
	
	@OneToMany(mappedBy = "telephonyPhone", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
	private List<TelephonyPhoneOrgUnit> orgUnits;
	
	@Column
	private String subscriptionType;
	
	@Column
	private String notes;
}