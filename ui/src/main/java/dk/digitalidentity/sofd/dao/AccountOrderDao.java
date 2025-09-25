package dk.digitalidentity.sofd.dao;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import dk.digitalidentity.sofd.dao.model.AccountOrder;
import dk.digitalidentity.sofd.dao.model.enums.AccountOrderStatus;
import dk.digitalidentity.sofd.dao.model.enums.AccountOrderType;

public interface AccountOrderDao extends CrudRepository<AccountOrder, Long> {

	List<AccountOrder> findAll();
	List<AccountOrder> findByStatusNotIn(Set<AccountOrderStatus> statuses);
	List<AccountOrder> findByStatusIn(Set<AccountOrderStatus> statuses);

	AccountOrder findById(long id);
	
	List<AccountOrder> findByStatusAndUserTypeAndOrderTypeAndActivationTimestampBefore(AccountOrderStatus status, String userType, AccountOrderType orderType, Date after);

	List<AccountOrder> findByStatusAndOrderTypeAndPersonUuid(AccountOrderStatus status, AccountOrderType orderType, String uuid);

	void deleteByStatusInAndPersonUuidInAndOrderTypeIn(Set<AccountOrderStatus> pending, Set<String> personUuids, List<AccountOrderType> types);

	List<AccountOrder> findByOrderTypeAndPersonUuid(AccountOrderType orderType, String personUuid);

	List<AccountOrder> findByOrderTypeIn(AccountOrderType ... types);

	List<AccountOrder> findByOrderTypeAndStatus(AccountOrderType create, AccountOrderStatus pending);

	List<AccountOrder> findByOrderTypeAndStatusAndUserType(AccountOrderType create, AccountOrderStatus created, String opusUserType);

	List<AccountOrder> findByPersonUuidAndOrderTypeAndStatusAndUserType(String uuid, AccountOrderType create, AccountOrderStatus created, String opusUserType);

	List<AccountOrder> findByOrderType(AccountOrderType orderType);

	List<AccountOrder> findByPersonUuidAndStatus(String personUuid, AccountOrderStatus status);

	<S extends AccountOrder> S save(S entity);
	
	AccountOrder findByDependsOn(AccountOrder dependsOn);

	List<AccountOrder> findByUserType(String userType);

	long countByUserTypeAndOrderTypeAndRequestedUserId(String userType, AccountOrderType orderType, String userId);

	List<AccountOrder> findByStatusAndUserTypeAndRequestedUserId(AccountOrderStatus status, String userType, String userId);
	
    List<AccountOrder> findByUserTypeAndOrderTypeAndStatusAndActualUserId(String userType, AccountOrderType orderType, AccountOrderStatus status, String actualUserId);

	/*
	Deletes pending create orders that are triggered by an affiliation that is no longer active.
	AccountOrder items that are related to deleted Affiliations are automatically deleted using cascade.
	EmailQueue items that are related to deleted Account Orders are automatically deleted using cascade.
	 */
	@Modifying
	@Query(nativeQuery = true, value = """
			delete ao
			from account_orders ao
			inner join affiliations a on a.id = ao.trigger_affiliation_id
			where
    			(a.deleted  = 1  or a.stop_date <= now())
				and	ao.status in ('PENDING','PENDING_APPROVAL')
				and ao.order_type = 'CREATE';
    	""")
	void cleanupByTriggerAffiliation();

	boolean existsByUserTypeAndOrderTypeAndRequestedUserIdAndStatusIn(String userType, AccountOrderType accountOrderType, String userId, List<AccountOrderStatus> pending);
}