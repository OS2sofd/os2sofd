package dk.digitalidentity.sofd.dao;

import java.util.Set;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import dk.digitalidentity.sofd.dao.model.Photo;

public interface PhotoDao extends CrudRepository<Photo, Long> {
    Photo findByPersonUuid(String personUuid);

    void deleteByPersonUuid(String personUuid);

    @Query(nativeQuery = true, value = "SELECT person_uuid FROM photos")
	Set<String> getPersonsWithPhotos();
}