package dk.digitalidentity.sofd.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dk.digitalidentity.sofd.dao.ClientConfigDao;
import dk.digitalidentity.sofd.dao.model.Client;
import dk.digitalidentity.sofd.dao.model.ClientConfig;

@Service
public class ClientConfigService {

	@Autowired
	private ClientConfigDao clientConfigDao;
	
	public ClientConfig save(ClientConfig clientConfig) {
		return clientConfigDao.save(clientConfig);
	}
	
	public List<ClientConfig> getAll() {
		return clientConfigDao.findAll();
	}
	
	public ClientConfig findByClient(Client client) {
		return clientConfigDao.findByClientId(client.getId());
	}
}
