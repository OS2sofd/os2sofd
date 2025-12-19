package dk.digitalidentity.sofd.dao;

import dk.digitalidentity.sofd.dao.model.Classification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClassificationDao extends JpaRepository<Classification, Long> {
	Optional<Classification> findByIdentifier(String identifier);
	boolean existsByIdentifier(String identifier);
}