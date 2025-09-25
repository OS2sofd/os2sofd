package dk.digitalidentity.sofd.dao.model;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.validation.constraints.NotNull;

import dk.digitalidentity.sofd.dao.model.enums.FacetType;
import lombok.Getter;
import lombok.Setter;

@Entity(name = "fh_facet")
@Getter
@Setter
public class Facet {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	
	@Column
	@NotNull
	private String name;
	
	@Column
	@NotNull
	@Enumerated(EnumType.STRING)
	private FacetType type;
	
	@Column
	private String description;
	
	@Column
	private String pattern;
	
	@OneToMany(mappedBy="facet", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<FacetListItem> listItems;
}
