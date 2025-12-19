package dk.digitalidentity.sofd.dao;

import dk.digitalidentity.sofd.dao.model.ClassificationItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClassificationItemDao extends JpaRepository<ClassificationItem, Long> {
	boolean existsByClassificationIdAndIdentifier(Long classificationId, String identifier);
	Optional<ClassificationItem> findByClassificationIdAndIdentifier(Long classificationId, String identifier);
}