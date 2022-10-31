package dk.digitalidentity.sofd.dao;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.springframework.data.repository.CrudRepository;

import dk.digitalidentity.sofd.dao.model.AccountOrder;
import dk.digitalidentity.sofd.dao.model.enums.AccountOrderStatus;
import dk.digitalidentity.sofd.dao.model.enums.AccountOrderType;

public interface AccountOrderDao extends CrudRepository<AccountOrder, Long> {

	List<AccountOrder> findAll();

	AccountOrder findById(long id);
	
	List<AccountOrder> findByStatusAndUserTypeAndOrderTypeAndActivationTimestampBefore(AccountOrderStatus status, String userType, AccountOrderType orderType, Date after);

	List<AccountOrder> findByStatusAndOrderTypeAndPersonUuid(AccountOrderStatus status, AccountOrderType orderType, String uuid);

	void deleteByStatusAndPersonUuidInAndOrderTypeIn(AccountOrderStatus pending, Set<String> personUuids, List<AccountOrderType> types);

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
}