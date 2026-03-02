package dk.digitalidentity.sofd.dao.model;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.BatchSize;
import org.hibernate.envers.NotAudited;

import dk.digitalidentity.sofd.dao.model.mapping.ProfessionMapping;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity(name = "professions")
@Getter
@Setter
@BatchSize(size = 50)
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
