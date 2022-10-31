package dk.digitalidentity.sofd.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.digitalidentity.sofd.dao.model.SupportedUserType;

public interface SupportedUserTypeDao extends JpaRepository<SupportedUserType, Long> {
	SupportedUserType findById(long id);
	List<SupportedUserType> findAll();
	SupportedUserType findByKey(String userType);
}
