package dk.digitalidentity.sofd.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dk.digitalidentity.sofd.dao.OrganisationDao;
import dk.digitalidentity.sofd.dao.model.Organisation;

@Service
public class OrganisationService {

    @Autowired
    private OrganisationDao organisationDao;

    public List<Organisation> getAll() {
        return organisationDao.findAll();
    }

    public Organisation getById(long id) {
    	if (id == 0) {
    		return organisationDao.findByShortName("ADMORG");
    	}

        return organisationDao.findById(id);
    }

    public Organisation getByShortName(String shortName) {
        return organisationDao.findByShortName(shortName);
    }

    public Organisation save(Organisation organisation) {
        return organisationDao.save(organisation);
    }

    public void delete(Organisation organisation) {
        organisationDao.delete(organisation);
    }

    public Organisation getAdmOrg() {
        return organisationDao.findByShortName("ADMORG");
    }

    public List<Organisation> getAllExceptAdmOrg() {
        return organisationDao.findByShortNameNot("ADMORG");
    }
}
