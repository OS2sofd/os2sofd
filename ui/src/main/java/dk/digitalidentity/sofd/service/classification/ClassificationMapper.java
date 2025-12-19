package dk.digitalidentity.sofd.service.classification;

import dk.digitalidentity.sofd.dao.model.Classification;
import dk.digitalidentity.sofd.dao.model.ClassificationItem;
import dk.digitalidentity.sofd.service.classification.model.ClassificationDTO;
import dk.digitalidentity.sofd.service.classification.model.ClassificationItemDTO;
import dk.digitalidentity.sofd.service.classification.model.ClassificationWithItemsDTO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ClassificationMapper {

    // Classification Entity → DTO (without items)
    public ClassificationDTO toDTO(Classification entity) {
        if (entity == null) {
            return null;
        }

        return new ClassificationDTO(
                entity.getIdentifier(),
                entity.getName()
        );
    }

    // Classification Entity → DTO (with items)
    public ClassificationWithItemsDTO toDTOWithItems(Classification entity) {
        if (entity == null) {
            return null;
        }

        List<ClassificationItemDTO> itemDTOs = entity.getItems().stream()
                .map(this::toItemDTO)
                .collect(Collectors.toList());

        return new ClassificationWithItemsDTO(
                entity.getIdentifier(),
                entity.getName(),
                itemDTOs
        );
    }

    // List<Classification> → List<ClassificationDTO>
    public List<ClassificationDTO> toDTOList(List<Classification> entities) {
        if (entities == null) {
            return null;
        }

        return entities.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ClassificationItem Entity → DTO
    public ClassificationItemDTO toItemDTO(ClassificationItem entity) {
        if (entity == null) {
            return null;
        }

        return new ClassificationItemDTO(
                entity.getIdentifier(),
                entity.getClassification().getIdentifier(),
                entity.getName()
        );
    }

    // DTO → Classification Entity (for create)
    public Classification toEntity(ClassificationDTO dto) {
        if (dto == null) {
            return null;
        }

        Classification entity = new Classification();
        entity.setIdentifier(dto.getIdentifier());
        entity.setName(dto.getName());

        return entity;
    }

    // Update existing entity from DTO (identifier is immutable, only name changes)
    public void updateEntityFromDTO(Classification entity, ClassificationDTO dto) {
        if (entity == null || dto == null) {
            return;
        }

        // Only update name, identifier is immutable
        entity.setName(dto.getName());
    }
}