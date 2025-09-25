package dk.digitalidentity.sofd.dao;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import dk.digitalidentity.sofd.dao.model.OrgUnitChange;
import dk.digitalidentity.sofd.dao.model.enums.OrgUnitChangeStatus;

public interface OrgUnitChangeDao extends CrudRepository<OrgUnitChange, Long> {
	List<OrgUnitChange> findByStatus(OrgUnitChangeStatus status);
}