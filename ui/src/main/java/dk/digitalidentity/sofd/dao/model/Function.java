package dk.digitalidentity.sofd.dao.model;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotNull;
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
