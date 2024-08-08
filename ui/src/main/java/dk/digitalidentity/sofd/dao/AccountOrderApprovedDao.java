package dk.digitalidentity.sofd.dao;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import dk.digitalidentity.sofd.dao.model.AccountOrderApproved;

public interface AccountOrderApprovedDao extends JpaRepository<AccountOrderApproved, Long> {

	List<AccountOrderApproved> findAll();
	AccountOrderApproved findById(long id);

	@Modifying
	@Query(nativeQuery = true, value = "DELETE FROM account_orders_approved WHERE approved_tts < ?1")
	void deleteByApprovedTtsBefore(LocalDateTime before);
}