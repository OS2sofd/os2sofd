package dk.digitalidentity.sofd.dao.model;

import java.time.LocalDate;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;

@Entity(name = "fh_function_assignment")
@Getter
@Setter
public class FunctionAssignment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	
	@Column
	private LocalDate startDate;

	@Column
	private LocalDate stopDate;
	
	@ManyToOne
	@JoinColumn(name = "affiliation_id", nullable = false)
	private Affiliation affiliation;
	
	@ManyToOne
	@JoinColumn(name = "function_id", nullable = false)
	private Function function;
	
	@OneToMany(mappedBy="functionAssignment", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<FacetValue> facetValues;
}
