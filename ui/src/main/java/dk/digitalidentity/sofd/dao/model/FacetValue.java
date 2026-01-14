package dk.digitalidentity.sofd.dao.model;

import java.time.LocalDate;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;

import lombok.Getter;
import lombok.Setter;

@Entity(name = "fh_facet_value")
@Getter
@Setter
public class FacetValue {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	
	@Column
	private String text;

	@Column
	LocalDate date;
	
	@ManyToOne
	@JoinColumn(name = "list_item_id", nullable = true)
	private FacetListItem facetListItem;
	
	@ManyToMany
	@JoinTable(name = "fh_facet_value_orgunit", joinColumns = @JoinColumn(name = "facet_value_id"), inverseJoinColumns = @JoinColumn(name = "orgunit_uuid"))
	private List<OrgUnit> orgUnits;
	
	@ManyToOne
	@JoinColumn(name = "affiliation_id", nullable = true)
	private Affiliation affiliation;
	
	@ManyToOne
	@JoinColumn(name = "facet_id", nullable = false)
	private Facet facet;
	
	@ManyToOne
	@JoinColumn(name = "function_assignment_id", nullable = false)
	private FunctionAssignment functionAssignment;
}
