package dk.digitalidentity.sofd.dao;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import dk.digitalidentity.sofd.dao.model.OrgUnitType;

public interface OrgUnitTypeDao extends CrudRepository<OrgUnitType, Long> {
	OrgUnitType findById(long id);
	List<OrgUnitType> findAll();
	List<OrgUnitType> findByActiveTrue();
	OrgUnitType findByKey(String key);
	OrgUnitType getByExtId(String extId);
}