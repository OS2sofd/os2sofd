package dk.digitalidentity.sofd.dao.model.mapping;

import com.fasterxml.jackson.annotation.JsonBackReference;
import dk.digitalidentity.sofd.dao.model.Profession;
import dk.digitalidentity.sofd.dao.model.enums.ProfessionMatchType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

@Entity(name = "profession_mappings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfessionMapping {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column
	private String matchValue;

	@Column
	@Enumerated(EnumType.STRING)
	private ProfessionMatchType matchType;

	@JsonBackReference
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "profession_id")
	private Profession profession;
}

