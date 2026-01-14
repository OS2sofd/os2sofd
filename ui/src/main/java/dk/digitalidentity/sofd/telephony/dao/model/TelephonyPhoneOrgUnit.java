package dk.digitalidentity.sofd.telephony.dao.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

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
