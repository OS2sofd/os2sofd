package dk.digitalidentity.sofd.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.digitalidentity.sofd.dao.model.ReservedUsername;

public interface ReservedUsernameDao extends JpaRepository<ReservedUsername, Long> {

	ReservedUsername findByPersonUuidAndEmployeeIdAndUserType(String uuid, String employeeId, String userType);

	long countByPersonUuidAndUserType(String uuid, String userType);

	ReservedUsername findByPersonUuidAndUserType(String uuid, String userType);

	List<ReservedUsername> findByPersonUuid(String uuid);

	void deleteByPersonUuid(String uuid);

}