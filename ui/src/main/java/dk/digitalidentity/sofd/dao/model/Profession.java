package dk.digitalidentity.sofd.dao.model;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.BatchSize;
import org.hibernate.envers.NotAudited;

import dk.digitalidentity.sofd.dao.model.mapping.ProfessionMapping;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity(name = "professions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Profession {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column
	private String name;

	@Column
	@NotNull
	private long organisationId;

	@NotAudited
	@BatchSize(size=100)
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true, mappedBy = "profession")
	private List<ProfessionMapping> professionMappings = new ArrayList<>();
}
