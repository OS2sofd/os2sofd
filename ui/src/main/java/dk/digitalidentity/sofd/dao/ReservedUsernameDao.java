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
        -- known_usernames is a history of names that have ever been used; the same-userType filter
        -- mirrors KnownUsernamesDao#findByUsernameAndUserType, and the :enforceKnownUsernames flag
        -- mirrors the isReuseExistingUsernames() gate in UsernameGeneratorService#validate.
        (:enforceKnownUsernames = true and exists (
            select 1 from known_usernames
            where username = :userId and user_type = :userType
            limit 1
        ))
        or exists (select 1 from reserved_usernames where user_id = :userId and person_uuid <> :personUuid limit 1)
        -- CREATED is included to bridge the gap between AD provisioning and the AD->SOFD sync
        -- inserting the User row; the 7 day cap lets the users-table check (below) take over once
        -- it catches up, so a long-lived CREATED order does not block the username forever
        -- (e.g. after the AD account has been deleted but the order row remains).
        --
        -- The (same user_type OR different person) predicate allows a person to share a user_id
        -- across user_types (e.g. Exchange reusing the AD user_id, see SAME_AS_OTHER infix logic)
        -- while still blocking duplicates within the same user_type and any cross-person collision.
        or exists (
            select 1 from account_orders
            where requested_user_id = :userId
              and order_type = 'CREATE'
              and (
                  status in ('PENDING', 'PENDING_APPROVAL', 'BLOCKED')
                  or (status = 'CREATED' and modified_timestamp > date_sub(now(), interval 7 day))
              )
              and (
                  user_type = :userType
                  or person_uuid <> :personUuid
              )
            limit 1
        )
        or exists (select 1 from bad_words where value = :userId limit 1)
        -- Same (same user_type OR different person) rule as above: a User row of a *different*
        -- user_type belonging to this person does not block (Exchange/AD share names by design),
        -- but a same-user_type row always blocks - you cannot have two AD accounts with the same
        -- user_id, even on the same person.
        or exists (
            select 1 from users u
            where u.user_id = :userId
              and (
                  u.user_type = :userType
                  or not exists (
                      select 1 from persons_users pu
                      where pu.user_id = u.id and pu.person_uuid = :personUuid
                  )
              )
            limit 1
        )
    """)
	Integer isIllegalGeneratedName(@Param("userId") String userId, @Param("userType") String userType, @Param("personUuid") String personUuid, @Param("enforceKnownUsernames") boolean enforceKnownUsernames);

	boolean existsByUserIdAndPersonUuidNot(String userId, String personUuid);

	@Query(nativeQuery = true, value = """
	select exists (select 1 from bad_words where value = :badWord limit 1)
	""")
	Integer isBadWord(@Param("badWord") String badWord);
}