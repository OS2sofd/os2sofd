package dk.digitalidentity.sofd.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.digitalidentity.sofd.dao.model.ActiveDirectoryDetails;

public interface ActiveDirectoryDetailsDao extends JpaRepository<ActiveDirectoryDetails, Long> {

	List<ActiveDirectoryDetails> findAll();
	ActiveDirectoryDetails findByUserId(long id);

}