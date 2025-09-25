package dk.digitalidentity.sofd.telephony.dao.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.envers.Audited;

import lombok.Getter;
import lombok.Setter;

@Entity(name = "telephony_phones_orgunits")
@Getter
@Setter
@Audited
public class TelephonyPhoneOrgUnit {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "telephony_phones_id")
	private TelephonyPhone telephonyPhone;

	@Column
	private String orgunitUuid;

	@Column
	private String orgunitName;
}
