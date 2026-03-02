package dk.digitalidentity.sofd.dao.model;

import org.hibernate.envers.Audited;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Audited
@Entity(name = "ean")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = "id", callSuper = true)
public class Ean extends MasteredEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column
	@NotNull
	private long number;

	@Column
	@NotNull
	private String master;

	@Column
	@NotNull
	private boolean prime;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "orgunit_uuid")
	@NotNull
	private OrgUnit orgUnit;

	@Override
	public String getMasterId() {
		return Long.toString(number);
	}

	@Override
	public void setMasterId(@NotNull String masterId) {
		;
	}
}
