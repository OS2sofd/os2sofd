package dk.digitalidentity.sofd.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dk.digitalidentity.sofd.dao.AccountOrderDao;
import dk.digitalidentity.sofd.dao.model.AccountOrder;
import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.SupportedUserType;
import dk.digitalidentity.sofd.dao.model.User;
import dk.digitalidentity.sofd.dao.model.enums.AccountOrderStatus;
import dk.digitalidentity.sofd.dao.model.enums.AccountOrderType;

/**
 * The discriminator that keeps "skip reactivation of former accounts" from also blocking deferred activation
 * ("udskudt aktivering"). A former account and an account waiting to be activated both present as a disabled
 * AD account on the person, and only the triggering affiliation on the completed CREATE order tells them apart.
 */
@ExtendWith(MockitoExtension.class)
public class AccountOrderServiceDeferredActivationTest {
	private static final String AD = "ACTIVE_DIRECTORY";
	private static final String PERSON_UUID = "9f2b0e1a-0000-0000-0000-000000000001";
	private static final String THIS_AFFILIATION = "1111aaaa-0000-0000-0000-000000000001";
	private static final String EARLIER_AFFILIATION = "2222bbbb-0000-0000-0000-000000000002";
	private static final String WAITING_USER_ID = "newacc";
	private static final String FORMER_USER_ID = "oldacc";

	@Mock
	private AccountOrderDao accountOrderDao;

	@InjectMocks
	private AccountOrderService accountOrderService;

	private SupportedUserType userType;
	private Affiliation affiliation;
	private User disabledUser;
	private User accountFromEarlierEmployment;

	@BeforeEach
	public void setup() {
		userType = new SupportedUserType();
		userType.setKey(AD);
		userType.setCreateAsDisabled(true);
		userType.setSkipReactivationOfFormerAccounts(true);

		Person person = new Person();
		person.setUuid(PERSON_UUID);

		affiliation = new Affiliation();
		affiliation.setUuid(THIS_AFFILIATION);
		affiliation.setPerson(person);

		disabledUser = new User();
		disabledUser.setUserType(AD);
		disabledUser.setUserId(WAITING_USER_ID);
		disabledUser.setDisabled(true);

		accountFromEarlierEmployment = new User();
		accountFromEarlierEmployment.setUserType(AD);
		accountFromEarlierEmployment.setUserId(FORMER_USER_ID);
		accountFromEarlierEmployment.setDisabled(true);
	}

	private AccountOrder completedCreateOrder(String actualUserId, String triggerAffiliationUuid) {
		AccountOrder order = new AccountOrder();
		order.setUserType(AD);
		order.setOrderType(AccountOrderType.CREATE);
		order.setStatus(AccountOrderStatus.CREATED);
		order.setPersonUuid(PERSON_UUID);
		order.setActualUserId(actualUserId);

		if (triggerAffiliationUuid != null) {
			Affiliation trigger = new Affiliation();
			trigger.setUuid(triggerAffiliationUuid);
			order.setTriggerAffiliation(trigger);
		}

		return order;
	}

	private void ordersInDatabase(List<AccountOrder> orders) {
		when(accountOrderDao.findByPersonUuidAndOrderTypeAndStatusAndUserType(anyString(), any(), any(), anyString()))
			.thenReturn(orders);
	}

	@Test
	public void theAccountThisAffiliationHadCreatedDisabledIsWaitingToBeActivated() {
		ordersInDatabase(List.of(completedCreateOrder(WAITING_USER_ID, THIS_AFFILIATION)));

		assertEquals(disabledUser, accountOrderService.findAwaitingDeferredActivation(List.of(disabledUser), affiliation, userType));
	}

	@Test
	public void aFormerAccountIsNotWaitingToBeActivated() {
		ordersInDatabase(List.of(completedCreateOrder(FORMER_USER_ID, EARLIER_AFFILIATION)));

		assertNull(accountOrderService.findAwaitingDeferredActivation(List.of(accountFromEarlierEmployment), affiliation, userType));
	}

	@Test
	public void anOrderForAnotherAccountDoesNotCount() {
		ordersInDatabase(List.of(completedCreateOrder(FORMER_USER_ID, THIS_AFFILIATION)));

		assertNull(accountOrderService.findAwaitingDeferredActivation(List.of(disabledUser), affiliation, userType));
	}

	@Test
	public void anOrderWithoutATriggeringAffiliationDoesNotCount() {
		ordersInDatabase(List.of(completedCreateOrder(WAITING_USER_ID, null)));

		assertNull(accountOrderService.findAwaitingDeferredActivation(List.of(disabledUser), affiliation, userType));
	}

	@Test
	public void noCompletedCreateOrdersMeansEveryDisabledAccountIsAFormerOne() {
		ordersInDatabase(Collections.emptyList());

		assertNull(accountOrderService.findAwaitingDeferredActivation(List.of(disabledUser), affiliation, userType));
	}

	/**
	 * The one that matters. A person who worked here before, and whose new account was just created in disabled
	 * state, has two disabled accounts at once. Picking whichever came first would either reactivate the former
	 * account or, worse, keep ordering new accounts night after night because the waiting one was never seen.
	 */
	@Test
	public void theWaitingAccountIsFoundEvenWhenAnOlderDisabledAccountComesFirst() {
		ordersInDatabase(List.of(
				completedCreateOrder(FORMER_USER_ID, EARLIER_AFFILIATION),
				completedCreateOrder(WAITING_USER_ID, THIS_AFFILIATION)));

		assertEquals(disabledUser, accountOrderService.findAwaitingDeferredActivation(
				List.of(accountFromEarlierEmployment, disabledUser), affiliation, userType));
	}

	/**
	 * AD does not care about casing on sAMAccountName, so the order and the imported user can legitimately
	 * disagree on it. Letting that decide the outcome would hand the person a second account every night.
	 */
	@Test
	public void casingOnTheUsernameDoesNotDecideTheOutcome() {
		ordersInDatabase(List.of(completedCreateOrder(WAITING_USER_ID.toUpperCase(), THIS_AFFILIATION)));

		assertEquals(disabledUser, accountOrderService.findAwaitingDeferredActivation(List.of(disabledUser), affiliation, userType));
	}

	@Test
	public void withoutDeferredActivationNoDisabledAccountIsEverWaitingToBeActivated() {
		userType.setCreateAsDisabled(false);

		assertNull(accountOrderService.findAwaitingDeferredActivation(List.of(disabledUser), affiliation, userType));

		// and we do not go to the database to work that out
		verifyNoInteractions(accountOrderDao);
	}

	@Test
	public void noDisabledAccountsAtAllSkipsTheLookup() {
		assertNull(accountOrderService.findAwaitingDeferredActivation(Collections.emptyList(), affiliation, userType));

		verifyNoInteractions(accountOrderDao);
	}

}
