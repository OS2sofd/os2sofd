package dk.digitalidentity.sofd.dao.model;

import java.time.LocalDate;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

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
