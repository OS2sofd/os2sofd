package dk.digitalidentity.sofd.dao;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import dk.digitalidentity.sofd.dao.model.Facet;

public interface FacetDao extends CrudRepository<Facet, Long> {

	List<Facet> findAll();

	Facet findById(long id);
}
