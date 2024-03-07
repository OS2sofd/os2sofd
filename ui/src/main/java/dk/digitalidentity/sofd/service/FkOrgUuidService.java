package dk.digitalidentity.sofd.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dk.digitalidentity.sofd.dao.FkOrgUuidDao;
import dk.digitalidentity.sofd.dao.model.FkOrgUuid;

@Service
public class FkOrgUuidService {

	@Autowired
	private FkOrgUuidDao fkOrgUuidDao;
	
	public FkOrgUuid save(FkOrgUuid fkOrgUuid) {
		return fkOrgUuidDao.save(fkOrgUuid);
	}
	
	public void delete(FkOrgUuid fkOrgUuid) {
		fkOrgUuidDao.delete(fkOrgUuid);
	}
	
	public List<FkOrgUuid> getByPersonUuid(String personUuid) {
		return fkOrgUuidDao.findByPersonUuid(personUuid);
	}
}
