package dk.digitalidentity.sofd.dao.model;

import java.util.List;

import dk.digitalidentity.sofd.dao.model.enums.FacetType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotNull;
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
