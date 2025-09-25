package dk.digitalidentity.sofd.dao.model;

import java.time.LocalDate;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;

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
