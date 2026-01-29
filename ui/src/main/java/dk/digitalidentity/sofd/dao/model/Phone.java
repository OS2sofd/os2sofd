package dk.digitalidentity.sofd.dao.model;

import org.hibernate.annotations.BatchSize;
import org.hibernate.envers.Audited;
import org.springframework.data.annotation.ReadOnlyProperty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import dk.digitalidentity.sofd.dao.model.enums.PhoneType;
import dk.digitalidentity.sofd.dao.model.enums.Visibility;
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
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity(name = "phones")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Audited
@BatchSize(size = 100)
@EqualsAndHashCode(exclude = "id", callSuper = true)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Phone extends MasteredEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column
	@NotNull
	private String master;

	@Column
	@NotNull
	private String masterId;

	@Column
	@NotNull
	@Size(max = 128)
	private String phoneNumber;

	@Column
	@Enumerated(EnumType.STRING)
	@NotNull
	private PhoneType phoneType;

	@Column
	private String notes;

	@Column
	@Enumerated(EnumType.STRING)
	private Visibility visibility;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "FunctionType_id")
	private FunctionType functionType;

	@ReadOnlyProperty
	@Column
	@NotNull
	private boolean prime;

	@Column
	@NotNull
	private boolean typePrime;

}
