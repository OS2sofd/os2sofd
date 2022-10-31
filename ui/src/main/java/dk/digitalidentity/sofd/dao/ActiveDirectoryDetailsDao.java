package dk.digitalidentity.sofd.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.digitalidentity.sofd.dao.model.ActiveDirectoryDetails;

public interface ActiveDirectoryDetailsDao extends JpaRepository<ActiveDirectoryDetails, Long> {

	ActiveDirectoryDetails findByUserId(long id);

}