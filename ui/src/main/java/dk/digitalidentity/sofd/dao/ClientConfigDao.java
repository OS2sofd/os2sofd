package dk.digitalidentity.sofd.dao;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import dk.digitalidentity.sofd.dao.model.ClientConfig;

public interface ClientConfigDao extends CrudRepository<ClientConfig, Long> {
	List<ClientConfig> findAll();
	ClientConfig findByClientId(long clientId);
}