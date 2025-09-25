package dk.digitalidentity.sofd.dao;

import dk.digitalidentity.sofd.dao.model.Institution;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstitutionDao extends JpaRepository<Institution, Long> {
}
