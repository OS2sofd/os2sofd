package dk.digitalidentity.sofd.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dk.digitalidentity.sofd.dao.SOFDAccountDao;
import dk.digitalidentity.sofd.dao.model.SOFDAccount;

@Service
public class SOFDAccountService {
	
	@Autowired
	private SOFDAccountDao sofdAccountDao;

	public List<SOFDAccount> findAll() {
		return sofdAccountDao.findAll();
	}

	public SOFDAccount findById(Long id) {
		return sofdAccountDao.findById(id).orElse(null);
	}

	public SOFDAccount save(SOFDAccount entity) {
		return sofdAccountDao.save(entity);
	}

	public SOFDAccount findByUserId(String userId) {
		return sofdAccountDao.findByUserId(userId);
	}

	public void delete(SOFDAccount entity) {
		sofdAccountDao.delete(entity);
	}
}
