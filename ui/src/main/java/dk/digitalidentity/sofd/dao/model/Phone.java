package dk.digitalidentity.sofd.dao.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.annotations.BatchSize;
import org.hibernate.envers.Audited;
import org.springframework.data.annotation.ReadOnlyProperty;

import dk.digitalidentity.sofd.dao.model.enums.PhoneType;
import dk.digitalidentity.sofd.dao.model.enums.Visibility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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
