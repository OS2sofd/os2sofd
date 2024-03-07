package dk.digitalidentity.sofd.dao;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import dk.digitalidentity.sofd.dao.model.OrgUnitManager;

public interface OrgUnitManagerDao extends CrudRepository<OrgUnitManager, String> {
	public List<OrgUnitManager> findByInheritedFalse();
}
