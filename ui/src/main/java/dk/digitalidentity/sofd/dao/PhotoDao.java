package dk.digitalidentity.sofd.dao;

import org.springframework.data.repository.CrudRepository;

import dk.digitalidentity.sofd.dao.model.Photo;

public interface PhotoDao extends CrudRepository<Photo, Long> {
    Photo findByPersonUuid(String personUuid);

    void deleteByPersonUuid(String personUuid);
}