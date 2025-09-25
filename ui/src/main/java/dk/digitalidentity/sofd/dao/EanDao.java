package dk.digitalidentity.sofd.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.digitalidentity.sofd.dao.model.Ean;

public interface EanDao extends JpaRepository<Ean, Long> {
	Ean findById(long id);
}
