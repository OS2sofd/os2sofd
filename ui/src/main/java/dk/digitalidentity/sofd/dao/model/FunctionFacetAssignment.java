package dk.digitalidentity.sofd.dao.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity(name = "fh_function_facet")
@Getter
@Setter
public class FunctionFacetAssignment {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column(name = "sort_key")
	private Long sortKey;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "function_id")
	@JsonBackReference
	private Function function;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "facet_id")
	private Facet facet;
}
