package dk.digitalidentity.sofd.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.digitalidentity.sofd.dao.model.SubstituteContext;

public interface SubstituteContextDao extends JpaRepository<SubstituteContext, Long> {
	List<SubstituteContext> findAll();

	SubstituteContext findById(long id);
}
