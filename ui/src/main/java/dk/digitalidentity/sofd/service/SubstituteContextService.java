package dk.digitalidentity.sofd.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dk.digitalidentity.sofd.dao.SubstituteContextDao;
import dk.digitalidentity.sofd.dao.model.SubstituteContext;

@Service
public class SubstituteContextService {

	@Autowired
	private SubstituteContextDao substituteContextDao;

	public List<SubstituteContext> getAll() {
		return substituteContextDao.findAll();
	}

	public SubstituteContext getById(long id) {
		return substituteContextDao.findById(id);
	}

	public SubstituteContext save(SubstituteContext substitute) {
		return substituteContextDao.save(substitute);
	}

	public void delete(SubstituteContext substitute) {
		substituteContextDao.delete(substitute);
	}
}
