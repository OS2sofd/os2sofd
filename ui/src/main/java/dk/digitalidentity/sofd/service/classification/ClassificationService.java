package dk.digitalidentity.sofd.service.classification;

import dk.digitalidentity.sofd.dao.ClassificationDao;
import dk.digitalidentity.sofd.dao.ClassificationItemDao;
import dk.digitalidentity.sofd.dao.model.Classification;
import dk.digitalidentity.sofd.dao.model.ClassificationItem;
import dk.digitalidentity.sofd.service.classification.model.ClassificationDTO;
import dk.digitalidentity.sofd.service.classification.model.ClassificationItemDTO;
import dk.digitalidentity.sofd.service.classification.model.ClassificationWithItemsDTO;
import dk.digitalidentity.sofd.service.classification.model.CreateClassificationItemRequest;
import dk.digitalidentity.sofd.service.classification.model.CreateClassificationRequest;
import dk.digitalidentity.sofd.service.classification.model.UpdateClassificationItemRequest;
import dk.digitalidentity.sofd.service.classification.model.UpdateClassificationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ClassificationService {

	private final ClassificationDao classificationDao;
	private final ClassificationItemDao classificationItemDao;
	private final ClassificationMapper mapper;

	/**
	 * Fetch all classifications (without items)
	 */
	@Transactional(readOnly = true)
	public List<ClassificationDTO> getAllClassifications() {
		List<Classification> classifications = classificationDao.findAll();
		return mapper.toDTOList(classifications);
	}

	/**
	 * Fetch a single classification by identifier (with items)
	 */
	@Transactional(readOnly = true)
	public ClassificationWithItemsDTO getClassificationByIdentifier(String identifier) {
		Classification classification = classificationDao.findByIdentifier(identifier)
				.orElseThrow(() -> new IllegalArgumentException(
						"Classification not found with identifier: " + identifier));
		return mapper.toDTOWithItems(classification);
	}

	/**
	 * Create a new classification
	 */
	public ClassificationDTO createClassification(CreateClassificationRequest request) {
		// Check if identifier already exists
		if (classificationDao.existsByIdentifier(request.getIdentifier())) {
			throw new IllegalArgumentException(
					"Classification already exists with identifier: " + request.getIdentifier());
		}

		Classification classification = new Classification();
		classification.setIdentifier(request.getIdentifier());
		classification.setName(request.getName());

		Classification saved = classificationDao.save(classification);
		return mapper.toDTO(saved);
	}

	/**
	 * Update an existing classification
	 */
	public ClassificationDTO updateClassification(String identifier, UpdateClassificationRequest request) {
		Classification classification = classificationDao.findByIdentifier(identifier)
				.orElseThrow(() -> new IllegalArgumentException(
						"Classification not found with identifier: " + identifier));

		classification.setName(request.getName());

		Classification updated = classificationDao.save(classification);
		return mapper.toDTO(updated);
	}

	/**
	 * Delete a classification
	 */
	public void deleteClassification(String identifier) {
		Classification classification = classificationDao.findByIdentifier(identifier)
				.orElseThrow(() -> new IllegalArgumentException(
						"Classification not found with identifier: " + identifier));

		classificationDao.delete(classification);
	}

	/**
	 * Get a single classification item by classification identifier and item identifier
	 */
	@Transactional(readOnly = true)
	public ClassificationItemDTO getClassificationItem(String classificationIdentifier, String itemIdentifier) {
		Classification classification = classificationDao.findByIdentifier(classificationIdentifier)
				.orElseThrow(() -> new IllegalArgumentException(
						"Classification not found with identifier: " + classificationIdentifier));

		ClassificationItem item = classificationItemDao.findByClassificationIdAndIdentifier(
						classification.getId(), itemIdentifier)
				.orElseThrow(() -> new IllegalArgumentException(
						"Classification item not found with identifier: " + itemIdentifier));

		return mapper.toItemDTO(item);
	}

	/**
	 * Create a classification item
	 */
	public ClassificationItemDTO createClassificationItem(CreateClassificationItemRequest request) {
		Classification classification = classificationDao.findByIdentifier(request.getClassificationIdentifier())
				.orElseThrow(() -> new IllegalArgumentException(
						"Classification not found with identifier: " + request.getClassificationIdentifier()));

		// Check if identifier already exists for this classification
		if (classificationItemDao.existsByClassificationIdAndIdentifier(
				classification.getId(), request.getIdentifier())) {
			throw new IllegalArgumentException(
					"Classification item already exists with identifier: " + request.getIdentifier());
		}

		ClassificationItem item = new ClassificationItem();
		item.setClassification(classification);
		item.setIdentifier(request.getIdentifier());
		item.setName(request.getName());

		ClassificationItem saved = classificationItemDao.save(item);
		return mapper.toItemDTO(saved);
	}

	/**
	 * Update a classification item
	 */
	public ClassificationItemDTO updateClassificationItem(String classificationIdentifier,
														  String itemIdentifier,
														  UpdateClassificationItemRequest request) {
		Classification classification = classificationDao.findByIdentifier(classificationIdentifier)
				.orElseThrow(() -> new IllegalArgumentException(
						"Classification not found with identifier: " + classificationIdentifier));

		ClassificationItem item = classificationItemDao.findByClassificationIdAndIdentifier(
						classification.getId(), itemIdentifier)
				.orElseThrow(() -> new IllegalArgumentException(
						"Classification item not found with identifier: " + itemIdentifier));

		// Only update name (identifier is immutable)
		item.setName(request.getName());

		ClassificationItem updated = classificationItemDao.save(item);
		return mapper.toItemDTO(updated);
	}

	/**
	 * Delete a classification item by classification identifier and item identifier
	 */
	public void deleteClassificationItem(String classificationIdentifier, String itemIdentifier) {
		Classification classification = classificationDao.findByIdentifier(classificationIdentifier)
				.orElseThrow(() -> new IllegalArgumentException(
						"Classification not found with identifier: " + classificationIdentifier));

		ClassificationItem item = classificationItemDao.findByClassificationIdAndIdentifier(
						classification.getId(), itemIdentifier)
				.orElseThrow(() -> new IllegalArgumentException(
						"Classification item not found with identifier: " + itemIdentifier));

		classificationItemDao.delete(item);
	}
}