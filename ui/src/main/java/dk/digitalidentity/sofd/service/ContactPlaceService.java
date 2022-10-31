package dk.digitalidentity.sofd.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dk.digitalidentity.sofd.dao.ContactPlacesDao;
import dk.digitalidentity.sofd.dao.model.ContactPlace;
import dk.digitalidentity.sofd.dao.model.OrgUnit;

@Service
public class ContactPlaceService {
	
	@Autowired
	private ContactPlacesDao contactPlacesDao;

	public List<ContactPlace> findAll() {
		return contactPlacesDao.findAll();
	}

	public ContactPlace findById(long id) {
		return contactPlacesDao.findById(id);
	}

	public ContactPlace save(ContactPlace contactPlace) {
		return contactPlacesDao.save(contactPlace);
	}

	public void delete(ContactPlace contactPlace) {
		contactPlacesDao.delete(contactPlace);
	}

	public ContactPlace findByContactPlace(OrgUnit ou) {
		return contactPlacesDao.findByContactPlace(ou);
	}

	public List<ContactPlace> findModified() {
		return contactPlacesDao.findBySynchronizedToOrganisationFalse();
	}
}
