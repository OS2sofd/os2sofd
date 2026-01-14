package dk.digitalidentity.sofd.dao.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

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
