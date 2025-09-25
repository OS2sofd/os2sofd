package dk.digitalidentity.sofd.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import dk.digitalidentity.sofd.dao.ClientDao;
import dk.digitalidentity.sofd.dao.model.Client;

@Service
@EnableScheduling
@EnableCaching
public class ClientService {

	@Autowired
	private ClientDao clientDao;

	@Autowired
	private ClientService self;

	// used by the ApiSecurityFilter class on every single API call, so caching will help a lot here
	@Cacheable(value = "clientList")
	public Client getClientByApiKey(String apiKey) {
		return clientDao.findByApiKey(apiKey);
	}

	public Client getClientByApiKeyBypassCache(String apiKey) {
		return clientDao.findByApiKey(apiKey);
	}
	
	public Client getClientByName(String name) {
		return clientDao.findByName(name);
	}

	public Client getClientById(long id) {
		return clientDao.findById(id);
	}

	public void delete(Client client) {
		clientDao.delete(client);
	}

	public Client save(Client client) {
		return clientDao.save(client);
	}

	public List<Client> findAll() {
		return clientDao.findAll();
	}

	public List<Client> findAllButInternals() {
		return clientDao.findByInternalFalse();
	}

	// run every 10 minutes
	@Scheduled(fixedRate = 1000 * 60 * 10)
	public void cacheClearTask() {
		self.cacheClear();
	}

	@CacheEvict(value = "clientList", allEntries = true)
	public void cacheClear() {
		; // do nothing, annotation handles actual logic
	}
}
