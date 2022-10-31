package dk.digitalidentity.sofd.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dk.digitalidentity.sofd.dao.KnownUsernamesDao;
import dk.digitalidentity.sofd.dao.model.KnownUsername;

@Service
public class KnownUsernamesService {

	@Autowired
	private KnownUsernamesDao knownUsernamesDao;
	
	public List<KnownUsername> findAll() {
		return knownUsernamesDao.findAll();
	}

	public KnownUsername findByUsernameAndUserType(String username, String userType) {
		return knownUsernamesDao.findByUsernameAndUserType(username, userType);
	}

	public void save(List<KnownUsername> entities) {
		knownUsernamesDao.saveAll(entities);
	}
}
