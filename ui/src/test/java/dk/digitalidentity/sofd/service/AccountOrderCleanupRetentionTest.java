package dk.digitalidentity.sofd.service;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.dao.AccountOrderDao;
import dk.digitalidentity.sofd.dao.model.AccountOrder;
import dk.digitalidentity.sofd.dao.model.enums.AccountOrderStatus;
import dk.digitalidentity.sofd.dao.model.enums.AccountOrderType;

/**
 * A completed CREATE order is the only record of which affiliation ordered a given account, and the nightly
 * generation reads it to tell an account waiting for deferred activation ("udskudt aktivering") apart from one
 * left over from an earlier employment. Letting the retention window delete it while the account is still waiting
 * would make the generation order a second account alongside the one already sitting there disabled.
 */
@ExtendWith(MockitoExtension.class)
public class AccountOrderCleanupRetentionTest {
	private static final int RETENTION_DAYS = 35;
	private static final String AD = "ACTIVE_DIRECTORY";
	private static final String PERSON_UUID = "9f2b0e1a-0000-0000-0000-000000000001";
	private static final String WAITING_USER_ID = "newacc";

	@Mock
	private AccountOrderDao accountOrderDao;

	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	private SofdConfiguration configuration;

	@InjectMocks
	private AccountOrderService accountOrderService;

	private AccountOrder completedCreate;

	@BeforeEach
	public void setup() {
		when(configuration.getModules().getAccountCreation().getAccountOrderRetentionDays()).thenReturn(RETENTION_DAYS);

		completedCreate = new AccountOrder();
		completedCreate.setId(1);
		completedCreate.setPersonUuid(PERSON_UUID);
		completedCreate.setUserType(AD);
		completedCreate.setOrderType(AccountOrderType.CREATE);
		completedCreate.setStatus(AccountOrderStatus.CREATED);
		completedCreate.setActualUserId(WAITING_USER_ID);
		completedCreate.setModifiedTimestamp(daysFromNow(-(RETENTION_DAYS + 5)));
	}

	private static Date daysFromNow(int days) {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_MONTH, days);

		return cal.getTime();
	}

	private AccountOrder reactivateOrder(AccountOrderStatus status, String requestedUserId, int activationInDays) {
		AccountOrder order = new AccountOrder();
		order.setId(2);
		order.setPersonUuid(PERSON_UUID);
		order.setUserType(AD);
		order.setOrderType(AccountOrderType.REACTIVATE);
		order.setStatus(status);
		order.setRequestedUserId(requestedUserId);
		order.setActivationTimestamp(daysFromNow(activationInDays));
		order.setModifiedTimestamp(daysFromNow(-(RETENTION_DAYS + 5)));

		return order;
	}

	@Test
	public void theCreateOrderSurvivesWhileItsAccountIsWaitingToBeActivated() {
		AccountOrder pendingReactivate = reactivateOrder(AccountOrderStatus.PENDING, WAITING_USER_ID, 10);
		when(accountOrderDao.findAll()).thenReturn(List.of(completedCreate, pendingReactivate));

		accountOrderService.cleanupOld();

		verify(accountOrderDao, never()).delete(completedCreate);
		verify(accountOrderDao, never()).delete(pendingReactivate);
	}

	@Test
	public void theMatchIgnoresCasingOnTheUsername() {
		when(accountOrderDao.findAll()).thenReturn(List.of(
				completedCreate,
				reactivateOrder(AccountOrderStatus.PENDING, WAITING_USER_ID.toUpperCase(), 10)));

		accountOrderService.cleanupOld();

		verify(accountOrderDao, never()).delete(completedCreate);
	}

	@Test
	public void aReactivateStillAwaitingApprovalAlsoProtectsTheCreateOrder() {
		when(accountOrderDao.findAll()).thenReturn(List.of(
				completedCreate,
				reactivateOrder(AccountOrderStatus.PENDING_APPROVAL, WAITING_USER_ID, 10)));

		accountOrderService.cleanupOld();

		verify(accountOrderDao, never()).delete(completedCreate);
	}

	@Test
	public void anOldCreateOrderWithNothingWaitingOnItIsStillDeleted() {
		when(accountOrderDao.findAll()).thenReturn(List.of(completedCreate));

		accountOrderService.cleanupOld();

		verify(accountOrderDao).delete(completedCreate);
	}

	@Test
	public void aReactivateThatAlreadyRanNoLongerProtectsTheCreateOrder() {
		when(accountOrderDao.findAll()).thenReturn(List.of(
				completedCreate,
				reactivateOrder(AccountOrderStatus.REACTIVATED, WAITING_USER_ID, -20)));

		accountOrderService.cleanupOld();

		verify(accountOrderDao).delete(completedCreate);
	}

	@Test
	public void aReactivateOnAnotherAccountDoesNotProtectTheCreateOrder() {
		when(accountOrderDao.findAll()).thenReturn(List.of(
				completedCreate,
				reactivateOrder(AccountOrderStatus.PENDING, "oldacc", 10)));

		accountOrderService.cleanupOld();

		verify(accountOrderDao).delete(completedCreate);
	}

	/**
	 * The exception cannot keep an order alive forever. A REACTIVATE that never ran is itself deleted once its own
	 * activation date is past the retention window, and the CREATE order it was protecting goes on the next run.
	 * Both are still read from the same snapshot here, so the CREATE order survives this one run and not the next.
	 */
	@Test
	public void aReactivateStuckLongEnoughIsDeletedAndStopsProtectingOnTheFollowingRun() {
		AccountOrder stuckReactivate = reactivateOrder(AccountOrderStatus.PENDING, WAITING_USER_ID, -(RETENTION_DAYS + 5));
		when(accountOrderDao.findAll()).thenReturn(List.of(completedCreate, stuckReactivate));

		accountOrderService.cleanupOld();

		verify(accountOrderDao).delete(stuckReactivate);
		verify(accountOrderDao, never()).delete(completedCreate);

		// the following night the stuck order is gone, and nothing is protecting the create order any more
		when(accountOrderDao.findAll()).thenReturn(List.of(completedCreate));

		accountOrderService.cleanupOld();

		verify(accountOrderDao).delete(completedCreate);
	}
}
