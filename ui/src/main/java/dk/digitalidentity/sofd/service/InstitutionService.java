package dk.digitalidentity.sofd.service;

import dk.digitalidentity.sofd.dao.InstitutionDao;
import dk.digitalidentity.sofd.dao.model.Institution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class InstitutionService {

	@Autowired
	private InstitutionDao institutionDao;

	public List<Institution> getAll() {
		return institutionDao.findAll();
	}

	public List<String> getAlInstitutionNumbers() {
		return institutionDao.findAll().stream().map(Institution::getInstitutionNumber).collect(Collectors.toList());
	}

	public Institution save(Institution institution) {
		return institutionDao.save(institution);
	}
}
