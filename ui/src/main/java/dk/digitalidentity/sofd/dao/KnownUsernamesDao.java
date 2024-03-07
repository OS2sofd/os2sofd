package dk.digitalidentity.sofd.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.digitalidentity.sofd.dao.model.KnownUsername;

import java.util.List;

public interface KnownUsernamesDao extends JpaRepository<KnownUsername, Long> {
	KnownUsername findByUsernameAndUserType(String username, String userType);
    List<KnownUsername> findByUsernameIgnoreCase(String username);
}
