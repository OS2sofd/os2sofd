package dk.digitalidentity.sofd.dao.model;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

@Entity(name = "fh_function")
@Getter
@Setter
public class Function {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	
	@Column
	@NotNull
	private String name;
	
	@Column
	private String category;
	
	@Column
	private String description;
	
	@Column(name = "sort_key")
	private int sortKey;
	
	@OneToMany(mappedBy="function", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<FunctionFacetAssignment> facetAssignments;
	
}
