package dk.digitalidentity.sofd.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.digitalidentity.sofd.dao.model.FkOrgUuid;

public interface FkOrgUuidDao extends JpaRepository<FkOrgUuid, Long> {
	
	List<FkOrgUuid> findByPersonUuid(String personUuid);

}
