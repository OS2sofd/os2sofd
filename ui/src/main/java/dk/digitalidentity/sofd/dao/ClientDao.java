package dk.digitalidentity.sofd.dao;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import dk.digitalidentity.sofd.dao.model.Client;

public interface ClientDao extends CrudRepository<Client, Long> {
	List<Client> findAll();
	Client findByApiKey(String apiKey);
	Client findByName(String name);
	Client findById(long id);
	List<Client> findByInternalFalse();


	@Modifying
	@Query(value = "UPDATE client c set last_active =:date where c.id = :clientId", nativeQuery = true)
	void setLastActive(@Param("date") Date date, @Param("clientId") long clientId);
}