package dk.digitalidentity.sofd.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.digitalidentity.sofd.dao.model.ContactPlace;
import dk.digitalidentity.sofd.dao.model.OrgUnit;

public interface ContactPlacesDao extends JpaRepository<ContactPlace, Long> {

	List<ContactPlace> findAll();

	ContactPlace findById(long id);

	ContactPlace findByContactPlace(OrgUnit ou);

	List<ContactPlace> findBySynchronizedToOrganisationFalse();
}
