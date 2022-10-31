package dk.digitalidentity.sofd.dao;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import dk.digitalidentity.sofd.dao.model.OrgUnitAccountOrder;

public interface OrgUnitAccountOrderDao extends CrudRepository<OrgUnitAccountOrder, Long> {
	List<OrgUnitAccountOrder> findAll();
	OrgUnitAccountOrder findByOrgunitUuid(String uuid);
}
