package dk.digitalidentity.sofd.dao;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import dk.digitalidentity.sofd.dao.model.Chart;

public interface OrganisationsdiagramDao extends CrudRepository<Chart, Long> {
	List<Chart> findAll();

	Chart findById(long id);

	Chart findByUuid(String uuid);
}