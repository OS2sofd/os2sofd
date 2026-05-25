package dk.digitalidentity.sofd.service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.dao.model.AccountOrder;
import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.OrgUnitAccountOrder;
import dk.digitalidentity.sofd.dao.model.OrgUnitAccountOrderType;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.SupportedUserType;
import dk.digitalidentity.sofd.dao.model.User;
import dk.digitalidentity.sofd.dao.model.enums.AccountOrderDeactivateAndDeleteRule;
import dk.digitalidentity.sofd.dao.model.enums.AccountOrderStatus;
import dk.digitalidentity.sofd.dao.model.enums.AccountOrderType;
import dk.digitalidentity.sofd.security.SecurityUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AccountOrderNightJob {

	@Autowired
	private SofdConfiguration configuration;

	@Autowired
	private AccountOrderService accountOrderService;
	
	@Autowired
	private PersonService personService;
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private SupportedUserTypeService supportedUserTypeService;
	
	@Autowired
	private AffiliationService affiliationService;
	
	@Transactional(readOnly = true)
	public void nightlyJob() {
		log.info("Starting nightly job - preloading data");

		Authentication authentication = SecurityUtil.getLoginSession();
		try {
			SecurityUtil.fakeLoginSession();

			long startTts = System.currentTimeMillis();

			List<AccountOrder> existingReactivateAndCreateOrders = accountOrderService.findAllCreateAndReactivateOrders();
			List<AccountOrder> existingDeactivateAndDeleteOrders = accountOrderService.findAllDeleteAndDeactivateOrders();

			List<Person> persons = personService.getActive();

			Map<String, User> activeDirectoryUserMap = new HashMap<>();
			List<AccountOrder> distinctCreateOrders = new ArrayList<>();
			List<AccountOrder> distinctDeleteDeactivateOrders = new ArrayList<>();
			int addedReactivateCreateOrders = 0;
			int addedDeleteDeactivateOrders = 0;
			int removedDeleteDeactivateOrders = 0;

			log.info("Processing " + persons.size() + " persons");

			// populate our AD map, that we will use later to manage cleanup on DELETE orders
			activeDirectoryUserMap.putAll(persons.stream()
				.flatMap(p -> p.getUsers().stream()).map(um -> um.getUser())
				.filter(u -> SupportedUserTypeService.isActiveDirectory(u.getUserType()))
				.collect(Collectors.toMap(User::getUserIdLowerCase, Function.identity()))
			);

			// fetch all affiliations for this batch
			List<Affiliation> affiliations = persons.stream()
				.flatMap(p -> p.getAffiliations().stream())
				.collect(Collectors.toList());

			// convert to easy lookup map
			Map<String, Person> allPersons = persons.stream().collect(Collectors.toMap(Person::getUuid, Function.identity()));

			// remove any affiliation that is not relevant for the IdM processes
			affiliations = accountOrderService.filterAffiliationsForCreateOrders(affiliations);

			log.info("Handling create orders for " + affiliations.size() + " affiliations");

			// NOTE: we are setting the "takeExistingAccounts" flag to false, so we ensure a clean set of
			//       orders, so any changes to the dataset (affiliations mostly) will result in old (unprocessed)
			//       orders being removed.
			List<AccountOrder> newReactivateAndCreateOrders = accountOrderService.getAccountsToCreate(affiliations, false, true);

			log.info("Got " + newReactivateAndCreateOrders.size() + " new orders");

			// remove any duplicate new orders
			List<AccountOrder> distinctNewCreateOrders = new ArrayList<>();
			newReactivateAndCreateOrders.forEach(newOrder -> {
				if (distinctNewCreateOrders.stream().noneMatch(distinctOrder -> distinctOrder.logicalEquals(newOrder))) {
					distinctNewCreateOrders.add(newOrder);
				}
			});

			log.info("Got " + distinctNewCreateOrders.size() + " distinct new create orders");
			
			// we need to keep track of ALL of them for later cleanup of existing orders
			distinctCreateOrders.addAll(distinctNewCreateOrders);

			List<AccountOrder> toSave = new ArrayList<>();

			// create those that are really new, and skip the rest
			for (AccountOrder newOrder : distinctNewCreateOrders) {
				boolean noMatch = existingReactivateAndCreateOrders.stream()
						.noneMatch(existingOrder -> newOrder.getPersonUuid().equals(existingOrder.getPersonUuid()) &&
								Objects.equals(newOrder.getEmployeeId(), existingOrder.getEmployeeId()) &&
								newOrder.getUserType().equals(existingOrder.getUserType()) &&
								!(existingOrder.getStatus().isComletedStatus() && (SupportedUserTypeService.isActiveDirectory(existingOrder.getUserType()) || SupportedUserTypeService.isExchange(existingOrder.getUserType())))
						);

				if (noMatch) {
					addedReactivateCreateOrders++;
					log.info("Adding " + newOrder.smallPrint());
					toSave.add(newOrder);
				}
			}

			log.info("Handling delete/deactivate orders for " + affiliations.size() + " affilations");

			List<AccountOrder> newDeleteOrders = getAccountsToDeleteOrDeactivate(persons, true);
			distinctDeleteDeactivateOrders.addAll(newDeleteOrders);

			log.info("Got " + newDeleteOrders.size() + " delete/deactivate orders");

			log.info("Looking to see which of " + newDeleteOrders.size() + " delete orders should be saved");

			// new deactivate/delete orders to be added to table
			for (AccountOrder newOrder : newDeleteOrders) {
				boolean noMatch = existingDeactivateAndDeleteOrders.stream()
						.noneMatch(existingOrder -> newOrder.getPersonUuid().equals(existingOrder.getPersonUuid()) &&
								newOrder.getOrderType().equals(existingOrder.getOrderType()) &&
								Objects.equals(newOrder.getEmployeeId(), existingOrder.getEmployeeId()) &&
								newOrder.getUserType().equals(existingOrder.getUserType()) &&
								Objects.equals(newOrder.getRequestedUserId(), existingOrder.getRequestedUserId()) &&
								existingOrder.getStatus().isPendingStatus());

				if (noMatch) {
					addedDeleteDeactivateOrders++;
					toSave.add(newOrder);
				}
			}
			
			if (toSave.size() > 0) {
				accountOrderService.saveAll(toSave, allPersons);
			}

			log.info("Deleting old create orders against that are no longer relevant");

			// remove existing create/reactivate orders that are no longer relevant
			int removedCreateOrders = 0;
			List<AccountOrder> toDelete = new ArrayList<>();
			for (AccountOrder existingOrder : existingReactivateAndCreateOrders) {

				// manual orders are not removed by the nightly job
				if (existingOrder.isManual()) {
					continue;
				}

				// skip cleanup on non-pending
				if (existingOrder.getStatus() != AccountOrderStatus.PENDING && existingOrder.getStatus() != AccountOrderStatus.PENDING_APPROVAL  ) {
					continue;
				}

				// status is now pending - compare with new orders - if we have a PENDING that is not in the new order-set, remove it

				boolean noMatch = distinctCreateOrders.stream()
						.noneMatch(newOrder -> newOrder.getPersonUuid().equals(existingOrder.getPersonUuid()) &&
								Objects.equals(newOrder.getEmployeeId(), existingOrder.getEmployeeId()) &&
								newOrder.getUserType().equals(existingOrder.getUserType()));

				if (noMatch) {
					removedCreateOrders++;
					log.info("Removing existing order: " + existingOrder.smallPrint());
					toDelete.add(existingOrder);
				}
			}

			if (toDelete.size() > 0) {
				accountOrderService.deleteAll(toDelete);
			}

			log.info("Deleteing delete/deactivate orders that are no longer relevant");

			// remove existing delete/deactivate (pending) orders that are no longer relevant
			for (AccountOrder existingOrder : existingDeactivateAndDeleteOrders) {

				// manual orders are not removed by the nightly job
				if (existingOrder.isManual()) {
					continue;
				}

				// skip cleanup on non-pending
				if (existingOrder.getStatus() != AccountOrderStatus.PENDING && existingOrder.getStatus() != AccountOrderStatus.PENDING_APPROVAL) {
					continue;
				}

				// delete orders on Active Directory accounts are not removed by the nightly job,
				// as the account might have been deactivated by a previous task-run, and then
				// this job would not know about the account, and thus not generate a delete job for it.
				if (existingOrder.getUserType().equals(SupportedUserTypeService.getActiveDirectoryUserType()) &&
					existingOrder.getOrderType().equals(AccountOrderType.DELETE)) {

					// we double-check against actual users, because the person might have been re-hired, and the old account re-activated,
					// and in that case, we do NOT want to delete the account
					User user = activeDirectoryUserMap.get(existingOrder.getRequestedUserId().toLowerCase());
					if (user != null && user.isDisabled()) {
						continue;
					}
				}

				var shouldDeleteOrder = false;
				var personOfOrder = personService.getByUuid(existingOrder.getPersonUuid());
				if (existingOrder.getOrderType() == AccountOrderType.DEACTIVATE && personOfOrder.isDisableAccountOrdersDisable()) {
					// delete the deactivate-order if person is exempt from deactivate-orders
					shouldDeleteOrder = true;
				}
				else if (existingOrder.getOrderType() == AccountOrderType.DELETE && personOfOrder.isDisableAccountOrdersDelete()) {
					// delete the delete-order if person is exempt from delete-orders
					shouldDeleteOrder = true;
				}
				else {
					// delete the order if it is no longer relevant (no match in newDeleteOrders)
					shouldDeleteOrder = distinctDeleteDeactivateOrders.stream()
							.noneMatch(newOrder -> newOrder.getPersonUuid().equals(existingOrder.getPersonUuid()) &&
									newOrder.getOrderType().equals(existingOrder.getOrderType()) &&
									Objects.equals(newOrder.getEmployeeId(), existingOrder.getEmployeeId()) &&
									newOrder.getUserType().equals(existingOrder.getUserType()));
				}

				if (shouldDeleteOrder) {
					log.info("Removing account order that is no longer relevant: " + existingOrder.getOrderType() + " on " + existingOrder.getRequestedUserId());
					removedDeleteDeactivateOrders++;
					accountOrderService.delete(existingOrder);
				}
			}

			log.info("Ordered the reactivation/creation of " + addedReactivateCreateOrders + " accounts, the deactivation/deletion of " + addedDeleteDeactivateOrders + " accounts, and cancelled " + (removedCreateOrders + removedDeleteDeactivateOrders) + " orders");

			long processingTime = System.currentTimeMillis() - startTts;
			if (processingTime > (10 * 60 * 1000)) {
				log.error("Processing nightjob took more than 10 minutes - total ms = " + processingTime);
			}
		}
		finally {
			SecurityUtil.setLoginSession(authentication);
		}
		
		log.info("Completed nightly job");
	}
	
	private List<AccountOrder> getAccountsToDeleteOrDeactivate(List<Person> persons, boolean respectDeleteDays) {
		List<AccountOrder> accountDeletesResult = new ArrayList<>();

		// read settings
		List<String> masters = configuration.getScheduled().getAccountOrderGeneration().getMasters();
		List<String> organisations = configuration.getScheduled().getAccountOrderGeneration().getOrganisations();

		// find orderable usertypes
		List<SupportedUserType> orderableUserTypesAsObjects = supportedUserTypeService.findAll().stream()
				.filter(u -> u.isCanOrder() && (u.isDeleteEnabled() || u.isDeactivateEnabled() ))
				.collect(Collectors.toList());

		Map<String, OffsetDays> offsetDays = new HashMap<>();
		for (SupportedUserType userType : orderableUserTypesAsObjects) {
			OffsetDays value = getOffsetDays(userType);
			if (value != null) {
				offsetDays.put(userType.getKey(), value);
			}
		}

		List<String> orderableUserTypes = orderableUserTypesAsObjects.stream().map(u -> u.getKey()).collect(Collectors.toList());

		Set<String> personUuidsWithAffiliationHistory = new HashSet<>();
		if (configuration.getScheduled().getAccountOrderGeneration().isIgnoreDeleteOrdersIfNoAffiliations()) {
			personUuidsWithAffiliationHistory = affiliationService.getPersonUuidsWithAffiliationHistory(masters);
		}

		for (Person person : persons) {
			// note we also grab deactivated users, as we might need to create DELETE orders for those
			List<User> users = PersonService.getUsers(person).stream().filter(u -> orderableUserTypes.contains(u.getUserType())).collect(Collectors.toList());

			// if OS2vikar is enabled, we skip all AD accounts with vikXXXX as the userId
			if (configuration.getModules().getSubstitute().isEnabled()) {
                users = users.stream().filter(u -> !UserService.isSubstituteUser(u)).collect(Collectors.toList());
			}

			// skip all OS2ILM AD accounts
			users = users.stream().filter(u -> !UserService.isOS2ilmUser(u)).collect(Collectors.toList());

			// ignore persons with no relevant user accounts
			if (users.size() == 0) {
				continue;
			}

			// if the person never had affiliations controlled by masters that are used in the IdM process (including aud table)
			// we skip deactivation - unless configured to generate those delete orders anyway
			if (configuration.getScheduled().getAccountOrderGeneration().isIgnoreDeleteOrdersIfNoAffiliations() && !personUuidsWithAffiliationHistory.contains(person.getUuid())) {
				continue;
			}

			for (User user : users) {
				if (respectDeleteDays && !offsetDays.containsKey(user.getUserType())) {
					continue;
				}

				// skip users that might be exempted
				if (userService.isIdmCloseExemptedUser(user)) {
					continue;
				}

				SupportedUserType supportedUserType = supportedUserTypeService.findByKey(user.getUserType());
				if (supportedUserType == null) {
					log.warn("Unknown userType when iterating over persons user accounts: " + user.getUserType());
					continue;
				}
				
				// find active affiliations of types that can affect account orders
				List<Affiliation> affiliations = person.getAffiliations().stream()
						.filter(a -> masters.contains(a.getMaster()) && organisations.contains(a.getCalculatedOrgUnit().getBelongsTo().getShortName()))
						.collect(Collectors.toList());

				affiliations = AffiliationService.notStoppedAffiliations(affiliations);

				// filter out affiliations according to orgunit account order delete/deactivate rules
				affiliations = CollectionUtils.emptyIfNull(affiliations).stream().filter(a -> shouldKeepAccountAlive(user.getUserType(),a)).collect(Collectors.toList());

				// if there are no active affiliations, we need to order delete/deactivate's
				boolean delete = (affiliations.size() == 0);

				// but even if we are not in the delete scenario, we might still be in the !singleAccount scenario,
				// where accounts are matched to affiliations, so we need to check if the matched affiliation has
				// expired, causing the user account be to flagged for deactivate/delete
				if (!delete && !supportedUserType.isSingleUserMode()) {
					boolean foundMatchingAffiliation = false;

					for (Affiliation affiliation : affiliations) {
						if (user.getEmployeeId() != null && user.getEmployeeId().equals(affiliation.getEmployeeId())) {
							foundMatchingAffiliation = true;
							break;
						}
					}

					delete = !(foundMatchingAffiliation);
				}

				if (delete) {
					Date deactivateDate = new Date();
					Date deleteDate = null;

					if (respectDeleteDays) {
						deactivateDate = offsetDays.get(user.getUserType()).deactivateDate;
						deleteDate = offsetDays.get(user.getUserType()).deleteDate;
					}
					else {
						// we only deactivate immediately, the delete will happen as scheduled in the future
						if (offsetDays.containsKey(user.getUserType())) {
							deleteDate = offsetDays.get(user.getUserType()).deleteDate;
						}
					}

					// for EXCHANGE we need to know the linked AD account
					String linkedUserId = null;
					if (SupportedUserTypeService.isExchange(user.getUserType())) {
						linkedUserId = user.getMasterId();
					}

					// no reason to deactivate already deactivated users ;)
					if (user.isDisabled() == false && deactivateDate != null && !person.isDisableAccountOrdersDisable()) {
						AccountOrder accountOrder = accountOrderService.deactivateOrDeleteAccountOrder(AccountOrderType.DEACTIVATE, person, user.getEmployeeId(), user.getUserType(), user.getUserId(), deactivateDate);
						accountOrder.setLinkedUserId(linkedUserId);
						accountDeletesResult.add(accountOrder);
					}

					if (deleteDate != null && !person.isDisableAccountOrdersDelete()) {
						AccountOrder accountOrder = accountOrderService.deactivateOrDeleteAccountOrder(AccountOrderType.DELETE, person, user.getEmployeeId(), user.getUserType(), user.getUserId(), deleteDate);
						accountOrder.setLinkedUserId(linkedUserId);
						accountDeletesResult.add(accountOrder);
					}
				}
			}
		}

		return accountDeletesResult;
	}

	private OffsetDays getOffsetDays(SupportedUserType supportedUserType) {
		// read initial settings
		// subtracting 1 day from these 2 since the deactivate/delete orders are always created 1 day after last employment date
		long daysToDeactivate = supportedUserType.getDaysToDeactivate() == 0 ? 0 : supportedUserType.getDaysToDeactivate() -1;
		long daysToDelete = supportedUserType.getDaysToDelete() == 0 ? 0 : supportedUserType.getDaysToDelete() -1;

		if (!supportedUserType.isDeactivateEnabled() && !supportedUserType.isDeleteEnabled()) {
			return null;
		}

		OffsetDays offsetDays = new OffsetDays();
		offsetDays.daysBeforeToCreate = supportedUserType.getDaysBeforeToCreate();

		// ordinary bulk actions are dealt with according to configured time (default 9:00)
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, configuration.getScheduled().getAccountOrderGeneration().getTimeOfExecution());
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND,0);
		Date todayAtNine = cal.getTime();

		if (supportedUserType.isDeactivateEnabled()) {
			cal.add(Calendar.DATE, (int) daysToDeactivate);
			offsetDays.deactivateDate = cal.getTime();
		}

		if (supportedUserType.isDeleteEnabled()) {
			cal.setTime(todayAtNine);
			cal.add(Calendar.DATE, (int) daysToDelete);
			offsetDays.deleteDate = cal.getTime();
		}

		return offsetDays;
	}
	
	private boolean shouldKeepAccountAlive(String userType, Affiliation affiliation) {
		if (affiliation.getDeactivateAndDeleteRule() != AccountOrderDeactivateAndDeleteRule.KEEP_ALIVE) {
			return false;
		}

		OrgUnitAccountOrder accountOrderSettings = accountOrderService.getAccountOrderSettings(affiliation.getCalculatedOrgUnit(), false);
		OrgUnitAccountOrderType accountOrderType = accountOrderSettings.getTypes().stream().filter(t -> t.getUserType().equalsIgnoreCase(userType)).findFirst().orElse(null);
		if (accountOrderType != null) {
            return switch (accountOrderType.getDeactivateAndDeleteRule()) {
                case KEEP_ALIVE -> true;
                case DEACTIVATE_AND_DELETE -> false;
                case DEACTIVATE_AND_DELETE_IF_HOURLY_PAID -> !affiliationService.isHourlyPaid(affiliation);
            };
		}

		return true;
	}

	class OffsetDays {
		long daysBeforeToCreate;
		Date deactivateDate;
		Date deleteDate;
	}

}
