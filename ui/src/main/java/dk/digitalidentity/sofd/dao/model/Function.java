package dk.digitalidentity.sofd.dao.model;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
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
	private String description;
	
	@Column(name = "sort_key")
	private int sortKey;
	
	@ManyToMany
	@JoinTable(name = "fh_function_facet", joinColumns = @JoinColumn(name = "function_id"), inverseJoinColumns = @JoinColumn(name = "facet_id"))
	private List<Facet> facets;
	
}
