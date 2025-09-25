package dk.digitalidentity.sofd.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import dk.digitalidentity.sofd.dao.model.ReservedUsername;

public interface ReservedUsernameDao extends JpaRepository<ReservedUsername, Long> {

	ReservedUsername findByPersonUuidAndEmployeeIdAndUserType(String uuid, String employeeId, String userType);

	long countByPersonUuidAndUserType(String uuid, String userType);

	ReservedUsername findByPersonUuidAndUserType(String uuid, String userType);

	List<ReservedUsername> findByPersonUuid(String uuid);

	void deleteByPersonUuid(String uuid);

	@Query(nativeQuery = true, value = """
    select
        exists (select 1 from known_usernames where username = :userId limit 1)
        or exists (select 1 from reserved_usernames where user_id = :userId limit 1)
        or exists (select 1 from account_orders where requested_user_id = :userId limit 1)
        or exists (select 1 from bad_words where value = :userId limit 1)
        or exists (select 1 from users where user_id = :userId limit 1)
    """)
	Integer isIllegalGeneratedName(@Param("userId") String userId);

	@Query(nativeQuery = true, value = """
	select exists (select 1 from bad_words where value = :badWord limit 1)
	""")
	Integer isBadWord(@Param("badWord") String badWord);
}