package dk.digitalidentity.sofd.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.digitalidentity.sofd.dao.model.Organisation;

public interface OrganisationDao extends JpaRepository<Organisation, Long> {

	void delete(Organisation entity);

	void deleteAll();

	List<Organisation> findByShortNameNot(String shortName);

	<S extends Organisation> List<S> save(Iterable<S> entities);

	<S extends Organisation> S save(S entity);

	<S extends Organisation> S saveAndFlush(S entity);

    List<Organisation> findAll();
    Organisation findById(long id);
    Organisation findByShortName(String shortName);
}