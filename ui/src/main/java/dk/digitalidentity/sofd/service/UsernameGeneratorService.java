package dk.digitalidentity.sofd.service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.querydsl.core.NonUniqueResultException;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.dao.ReservedUsernameDao;
import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.ReservedUsername;
import dk.digitalidentity.sofd.dao.model.SupportedUserType;
import dk.digitalidentity.sofd.dao.model.User;
import dk.digitalidentity.sofd.dao.model.enums.AccountOrderType;
import dk.digitalidentity.sofd.dao.model.enums.AffiliationType;
import dk.digitalidentity.sofd.dao.model.enums.UsernameInfixType;
import dk.digitalidentity.sofd.service.transliteration.Transliteration;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class UsernameGeneratorService {
    private static final char[] legalChars = "qwertyuioplkjhgfdsazxcvbnm".toCharArray();
    private static final SecureRandom random = new SecureRandom();
    
	@Autowired
	private UserService userService;
	
	@Autowired
	private BadWordsService badWordsService;
	
	@Autowired
	private KnownUsernamesService knownUsernamesService;
	
	@Autowired
	private SofdConfiguration configuration;
	
	@Autowired
	private ReservedUsernameDao reservedUsernameDao;
	
	@Autowired
	private SupportedUserTypeService supportedUserTypeService;

	@Autowired
	private SettingService settingService;

	@Autowired
	private AccountOrderService accountOrderService;

	public String getUsername(Person person, String employeeId, String userType, String linkedUserId) {
		String userId = null;
		
		if (configuration.getModules().getAccountCreation().isReservationEnabled()) {
			ReservedUsername reservedUsername = null;
			try {
				reservedUsername = getReservedUsername(person, employeeId, userType);
			}
			catch (Exception ex) {
				log.error("Failed to generate username reservation for " + person.getUuid() + " / " + employeeId + " / " + userType);
				throw ex;
			}
	
			if (reservedUsername == null) {
				return null;
			}

			userId = reservedUsername.getUserId();
		}
		else {
			Affiliation affiliation = (StringUtils.hasLength(employeeId))
					? person.getAffiliations().stream().filter(a -> Objects.equals(a.getEmployeeId(), employeeId)).findFirst().orElse(null)
					: AffiliationService.notStoppedAffiliations(person.getAffiliations()).stream().findFirst().orElse(null);

			if (affiliation == null) {
				log.warn("Person has no affiliations: " + person.getUuid());
				return null;
			}
			
			SupportedUserType supportedUserType = supportedUserTypeService.findByKey(userType);
			if (supportedUserType == null) {
				log.error("Not a supported userType: " + userType);
				return null;
			}
			
			userId = generateUsername(supportedUserType, affiliation, person, new ArrayList<>());
			if (userId == null) {
				log.warn("Generation not possible for " + person.getUuid());
				return null;
			}
			
			// and special check, to make sure there are no pending orders for this username (not checked by the generator, because it relies on the
			// reservation system to ensure this)
			if (accountOrderService.countByUserTypeAndOrderTypeAndRequestedUserId(userType, AccountOrderType.CREATE, userId) > 0) {
				throw new RuntimeException("Brugernavngeneratoren valgte '" + userId + "' som brugerId, men der ligger allerede en afventendre ordre med det brugernavn - vælg det ønskede brugernavn og kør ordren igen");
			}
		}
		
		// verify that the returned reserved username does not conflict with some dependency rule
		// note that this can happen if the reserved username was created before the account it
		// depends on was created - and that account was created manually for some reason (breaking
		// the reserved username rule)
		SupportedUserType supportedUserType = supportedUserTypeService.findByKey(userType);
		if (supportedUserType != null) {
			if (supportedUserType.getUsernameInfix().equals(UsernameInfixType.SAME_AS_OTHER)) {
				SupportedUserType sameAsType = supportedUserTypeService.findById(Long.parseLong(supportedUserType.getUsernameInfixValue()));

				// in case of DSCP-85 (fallback rule for OPUS), we skip this validation
				boolean dscp85 = false;
				if (SupportedUserTypeService.isOpus(supportedUserType.getKey()) && supportedUserType.isSingleUserMode() == false && sameAsType.isSingleUserMode() == true) {
					dscp85 = true;
				}
				
				// this is a special case, where the following is true
				// * the account is an EXCHANGE account
				// * EXCHANGE accounts are configured to have the same userId as the linked AD account
				// * a linked AD account i supplied
				//
				// In this case, we ignore the generated/reserved username, and just returned the linked account
				if (SupportedUserTypeService.isExchange(supportedUserType.getKey()) &&
					SupportedUserTypeService.isActiveDirectory(sameAsType.getKey()) &&
					StringUtils.hasLength(linkedUserId)) {

					if (!Objects.equals(userId, linkedUserId)) {
						log.warn("Reserved userId for exchange account was " + userId + " but it was linked to AD account with userId " + linkedUserId + " so the reservation was ignored for: " + person.getUuid());
					}
					
					return linkedUserId;
				}

				if (!dscp85) {
					List<String> userIdsOfType = PersonService.getUsers(person).stream()
							.filter(u -> u.getUserType().equals(sameAsType.getKey()))
							.map(u -> u.getUserId().toLowerCase())
							.collect(Collectors.toList());
					
					// if the person has a user of the type that is depended on, but none of these
					// has a userId that is contained within the reserved userId, then most likely
					// someone manually generated this account, and it does NOT match the reserved
					// userId, so we have to ship this for manual handling
					if (userIdsOfType.size() > 0) {
						final String fUserId = userId.toLowerCase();
						
						if (!userIdsOfType.stream().anyMatch(u -> fUserId.contains(u))) {
							log.warn("Failed to retrieve username for " + getName(person) + " / " + person.getUuid() + " because userType " + userType + " depends on " + supportedUserType.getUsernameInfixValue() + " and there was a previous reserved username that did not match the existing account");
							return null;
						}
					}
				}
			}
		}

		return userId;
	}
	
	private ReservedUsername getReservedUsername(Person person, String employeeId, String userType) {
		return getReservedUsername(person, employeeId, userType, true);
	}

	private ReservedUsername getReservedUsername(Person person, String employeeId, String userType, boolean firstTry) {
		ReservedUsername reservedUsername = null;
		
		SupportedUserType supportedUserType = supportedUserTypeService.findByKey(userType);
		if (supportedUserType == null) {
			log.warn("Unknown usertype in getReservedUsername: " + userType);
			return reservedUsername;
		}

		try {
			if (!supportedUserType.isSingleUserMode()) {
				reservedUsername = reservedUsernameDao.findByPersonUuidAndEmployeeIdAndUserType(person.getUuid(), employeeId, userType);
			}
			else {
				try {
					reservedUsername = reservedUsernameDao.findByPersonUuidAndUserType(person.getUuid(), userType);
				}
				catch (NonUniqueResultException | IncorrectResultSizeDataAccessException ignored) {
					// special case - we MIGHT have a employeeId, and that could be used to avoid the nonUnique problem
					reservedUsername = reservedUsernameDao.findByPersonUuidAndEmployeeIdAndUserType(person.getUuid(), employeeId, userType);
					if (reservedUsername == null) {
						log.warn("attempting to lookup using employeeId " + employeeId + " did not help, got NULL");
						return null;
					}
				}
			}
		}
		catch (Exception ex) {
			log.error("Failed to get a reserved username for: " + person.getUuid());
			throw ex;
		}
		
		if (reservedUsername != null && SupportedUserTypeService.isActiveDirectory(userType)) {
			User user = userService.findByUserIdAndUserType(reservedUsername.getUserId(), userType);
			
			if (user != null && !user.isDisabled()) {
				String generatedUsername = generateUsername(supportedUserType, new Affiliation(), person, reservedUsernameDao.findByPersonUuid(person.getUuid()));
				ReservedUsername username = new ReservedUsername();
				username.setEmployeeId(employeeId);
				username.setUserId(generatedUsername);
				username.setPersonUuid(person.getUuid());
				username.setUserType(userType);
				return username;
			}
		}
		
		// if none exists, reserve and try again
		if ((reservedUsername == null && firstTry)) {
			reserveUsernames(person);
			
			reservedUsername = getReservedUsername(person, employeeId, userType, false);
		}
		
		return reservedUsername;
	}

	private void reserveUsernames(Person person) {

		// TODO: nope, this is also called when saving a brand new person, and that triggers a world of hurt
		// before reserving username, make an update from cpr to ensure we have updated names to generate username from
		// cprUpdateService.updatePerson(person.getUuid());

		if (!person.hasName()) {
			log.warn("Did not reserve usernames for person with uuid " + person.getUuid() + " because the person has no name.");
			return;
		}
		List<ReservedUsername> reservedUsernames = reservedUsernameDao.findByPersonUuid(person.getUuid());
		
		List<Affiliation> activeAffiliations = AffiliationService.notStoppedAffiliations(person.getAffiliations());
		List<Affiliation> activeOpusAffiliations = activeAffiliations.stream().filter(a -> "OPUS".equals(a.getMaster())).collect(Collectors.toList());
		
		List<SupportedUserType> userTypesWithoutReservations = new ArrayList<>();
		for (SupportedUserType supportedUserType : supportedUserTypeService.findAll()) {
			
			// skip those that cannot be ordered
			if (!supportedUserType.isCanOrder()) {
				continue;
			}

			// skip those that we have already reserved usernames for
			if (supportedUserType.isSingleUserMode()) {
				if (reservedUsernames.stream().map(r -> r.getUserType()).anyMatch(ut -> ut.equals(supportedUserType.getKey()))) {
					continue;
				}			
			}
			else {
				Set<String> employeeIds = new HashSet<>();

				if (SupportedUserTypeService.isOpus(supportedUserType.getKey())) {
					employeeIds = activeOpusAffiliations.stream().map(a -> a.getEmployeeId()).collect(Collectors.toSet());
				}
				else {
					// without OPUS affiliations, we do not have employeeIds, so all we can do is add NULL
					// as the employeeId... it also means we can only reserve ONE userId in these cases
					employeeIds.add(null);
				}

				// check if any employeeIds is missing a reserved username
				boolean missing = false;
				for (String employeeId : employeeIds) {
					long count = reservedUsernames.stream().filter(r -> r.getUserType().equals(supportedUserType.getKey()) &&
																		Objects.equals(r.getEmployeeId(), employeeId)).count();
					
					if (count == 0) {
						missing = true;
						break;
					}
				}
				
				if (!missing) {
					continue;
				}
			}

			userTypesWithoutReservations.add(supportedUserType);
		}
		
		if (userTypesWithoutReservations.size() == 0) {
			return;
		}

		// sort them by dependency
		userTypesWithoutReservations.stream().sorted((u1, u2) -> {
			if (u1.getDependsOn() != null && u1.getDependsOn().equals(u2)) {
				return 1;
			}
			else if (u2.getDependsOn() != null && u2.getDependsOn().equals(u1)) {
				return -1;
			}
			
			return 0;
		});

		for (SupportedUserType userType : userTypesWithoutReservations) {			
			// start by generating a list of affiliations to be used for the username generation logic
			// for non-singleuser cases, we need all affiliations, for the more common (singleuser) case,
			// we use the prime affiliation
			List<Affiliation> affiliations = activeAffiliations;
			
			// for OPUS accounts, we need to filter out non-OPUS affiliations
			if (SupportedUserTypeService.isOpus(userType.getKey())) {
				affiliations = activeOpusAffiliations;
				
				// for OPUS accounts, at least one affiliation is required, so skip if not available
				if (affiliations.size() == 0) {
					continue;
				}
			}

			// ensure a single affiliation is available in single-user-mode
			if (userType.isSingleUserMode()) {
				Optional<Affiliation> primeAffiliation = affiliations.stream().filter(a -> a.isPrime()).findFirst();

				if (primeAffiliation.isPresent()) {
					// if the prime affiliation is not from OPUS, try to see if we can find an OPUS affiliation to use instead
					if (!"OPUS".equals(primeAffiliation.get().getMaster())) {
						Optional<Affiliation> opusAffiliation = affiliations.stream().filter(a -> "OPUS".equals(a.getMaster())).findFirst();
						if (opusAffiliation.isPresent()) {
							primeAffiliation = opusAffiliation;
						}
					}

					affiliations = Collections.singletonList(primeAffiliation.get());
				}
				else {
					// no prime affiliation, pick one at random (can happen for OPUS account types, where a SOFD affiliation is prime)
					if (affiliations.size() > 0) {
						affiliations = Collections.singletonList(affiliations.get(0));
					}
					else {
						// add a single dummy affiliation, to ensure the loop below runs once
						affiliations = new ArrayList<>();
						Affiliation affiliation = new Affiliation();
						affiliations.add(affiliation);
					}
				}
			}
			
			// note that in the single-user mode this loop has one (and only one) entry
			// and that employeeIds has a single null-entry
			for (Affiliation affiliation : affiliations) {
				
				// in the non-single-user-mode, we might already have a reservation for this specific affiliation, so check that
				if (!userType.isSingleUserMode() && StringUtils.hasLength(affiliation.getEmployeeId())) {
					boolean existingReservation = reservedUsernames.stream().anyMatch(r -> Objects.equals(r.getUserType(), userType.getKey()) && Objects.equals(r.getEmployeeId(), affiliation.getEmployeeId()));
					
					if (existingReservation) {
						continue;
					}
				}

				// if the user already has a user of that type, just use the userId from that user as the reserved username
				if (userType.isSingleUserMode()) {
					Optional<User> user = PersonService.getUsers(person).stream().filter(u -> u.getUserType().equals(userType.getKey()) && u.isPrime()).findFirst();
					if (user.isPresent()) {
						String userId = user.get().getUserId();
						
						// for exchange, strip out the @ part
						if (SupportedUserTypeService.isExchange(userType.getKey())) {
							int idx = userId.indexOf("@");
							if (idx > 0) {
								userId = userId.substring(0, idx);
							}
						}

						ReservedUsername reservedUsername = new ReservedUsername();
						reservedUsername.setPersonUuid(person.getUuid());
						reservedUsername.setUserId(userId);
						reservedUsername.setUserType(userType.getKey());
						reservedUsernameDao.save(reservedUsername);

						// update local list
						reservedUsernames.add(reservedUsername);
						
						// done with this userType
						continue;
					}
				}
				else {
					Optional<User> user = PersonService.getUsers(person).stream()
							.filter(u -> u.getUserType().equals(userType.getKey()) && Objects.equals(u.getEmployeeId(), affiliation.getEmployeeId()))
							.findFirst();

					if (user.isPresent()) {
						String userId = user.get().getUserId();
						
						// for exchange, strip out the @ part
						if (SupportedUserTypeService.isExchange(userType.getKey())) {
							int idx = userId.indexOf("@");
							if (idx > 0) {
								userId = userId.substring(0, idx);
							}
						}

						ReservedUsername reservedUsername = new ReservedUsername();
						reservedUsername.setPersonUuid(person.getUuid());
						reservedUsername.setEmployeeId(user.get().getEmployeeId());
						reservedUsername.setUserId(userId);
						reservedUsername.setUserType(userType.getKey());
						reservedUsernameDao.save(reservedUsername);

						// update local list
						reservedUsernames.add(reservedUsername);

						// done with this userType
						continue;
					}
				}

				String generatedUsername = generateUsername(userType, affiliation, person, reservedUsernames);
				
				// infix default value is "" - if it is null, the generator failed
				if (generatedUsername == null) {
					continue;
				}

				ReservedUsername reservedUsername = new ReservedUsername();
				reservedUsername.setEmployeeId(affiliation.getEmployeeId());
				reservedUsername.setPersonUuid(person.getUuid());
				reservedUsername.setUserId(generatedUsername);
				reservedUsername.setUserType(userType.getKey());
				reservedUsernameDao.save(reservedUsername);

				// update local list
				reservedUsernames.add(reservedUsername);
			}
		}
	}
	
	private String generateUsername(SupportedUserType userType, Affiliation affiliation, Person person, List<ReservedUsername> reservedUsernames) {
		if( !person.hasName())
		{
			log.warn("Did not generate username for person with uuid " + person.getUuid() + " because the person has no name.");
			return null;
		}

		String prefix = "";
		switch (userType.getUsernamePrefix()) {
			case CREATE_DATE:
				prefix = LocalDate.now().format(DateTimeFormatter.ofPattern("ddMM"));
				break;
			case VALUE:
				if (affiliation.getAffiliationType() == AffiliationType.EXTERNAL && userType.getUsernamePrefixExternalValue() != null && !userType.getUsernamePrefixExternalValue().isBlank()) {
					prefix = userType.getUsernamePrefixExternalValue().trim(); 
				} else {
					prefix = (userType.getUsernamePrefixValue() != null) ? userType.getUsernamePrefixValue().trim() : "";
				}
				break;
			case NONE:
				break;
		}
		
		String suffix = "";
		switch (userType.getUsernameSuffix()) {
			case CREATE_DATE:
				suffix = LocalDate.now().format(DateTimeFormatter.ofPattern("ddMM"));
				break;
			case VALUE:
				if (affiliation.getAffiliationType() == AffiliationType.EXTERNAL && userType.getUsernameSuffixExternalValue() != null && !userType.getUsernameSuffixExternalValue().isBlank()) {
					suffix = userType.getUsernameSuffixExternalValue().trim();
				} else {
					suffix = (userType.getUsernameSuffixValue() != null) ? userType.getUsernameSuffixValue().trim() : "";
				}
				break;
			case NONE:
				break;
		}

		String infix = "";
		switch (userType.getUsernameInfix()) {
			case EMPLOYEE_ID:
				if (affiliation.getEmployeeId() == null) {
					log.info("Failed to generate username for " + person.getUuid() + " due to empty employeeId for type: " + userType.getName());
					return null;
				}

				infix = affiliation.getEmployeeId();
				break;
			case FROM_NAME:
				infix = shortName(person, userType.getKey(), getLong(userType.getUsernameInfixValue(), 5), prefix, suffix);
				break;
			case FROM_NAME_LONG:
				infix = longName(person, userType.getKey(), prefix, suffix, false);
				break;
			case FROM_NAME_FULL:
				infix = longName(person, userType.getKey(), prefix, suffix, true);
				break;
			case RANDOM:
				infix = random(userType.getKey(), getLong(userType.getUsernameInfixValue(), 5), prefix, suffix);
				break;
			case NUMBER:
				infix = number(userType.getKey(), getLong(userType.getUsernameInfixValue(), 5), prefix, suffix);
				break;
			case NAME23SERIAL:
				infix =  name23serial(person, userType.getKey(), prefix, suffix);
				break;
			case SAME_AS_OTHER:
				SupportedUserType otherUserType = supportedUserTypeService.findById(Long.parseLong(userType.getUsernameInfixValue()));
				if (otherUserType == null) {
					log.error("Failed to find userType with id " + userType.getUsernameInfixValue());
				}
				else {
					boolean found = false;

					// DSCP-85
					//
					// special corner case for OPUS accounts. If a municipality runs OPUS in non-singleUserMode,
					// and they want the OPUS name to depend on another account-type (e.g. AD), but that accountType
					// runs in SingleUserMode, then we perform a fallback, and use the employeeId for all except
					// the first OPUS account
					boolean dscp85 = false;
					if (SupportedUserTypeService.isOpus(userType.getKey()) &&
						!userType.isSingleUserMode() &&
						otherUserType.isSingleUserMode() &&
						reservedUsernameDao.countByPersonUuidAndUserType(person.getUuid(), SupportedUserTypeService.getOpusUserType()) > 0) {

						dscp85 = true;
					}

					if (!dscp85) { // default
						for (ReservedUsername reservedUsername : reservedUsernames) {
							if (reservedUsername.getUserType().equals(otherUserType.getKey())) {
								infix = reservedUsername.getUserId();
								found = true;
								break;
							}
						}

						if (!found) {
							infix = null;
							log.warn("No reserved username for '" + otherUserType.getKey() + "', so unable to reserve username for '" + userType.getKey() + "' which depends on the previous, for " + person.getUuid());
						}
					}
					else { // fallback for DSCP-85 case
						infix = affiliation.getEmployeeId();
					}
				}
				
				break;
		}
		
		// infix default value is "" - if it is null, the generator failed
		if (infix == null) {
			return null;
		}
		
		return prefix + infix + suffix;
	}
	
	private long getLong(String value, long defaultValue) {
		try {
			return Long.parseLong(value);
		}
		catch (Exception ex) {
			log.error("Failed to parse value: " + value);
		}

		return defaultValue;
	}

	private String random(String userType, long len, String prefix, String suffix) {
		int maxTries = 10;
		
		String username = null;
		while (maxTries-- > 0) {
			String word = randomWord(len);

			if (isRejected(word, userType, prefix, suffix)) {
				continue;
			}

			username = word;
			break;
		}

		return username;
	}

	private String number(String userType, long len, String prefix, String suffix) {
		int maxTries = 50;
		var number = settingService.getLastUserNameNumberUsed(userType) + 1;

		String username = null;
		// maxtries should not really be necessary here, as we just keep incrementing the number
		// but we limit amount of retries anyway...just in case.
		while (maxTries-- > 0) {
			// zero-pad the number
			String paddedNumber = String.format("%0" + String.valueOf(len) + "d", number);

			if (isRejected(paddedNumber, userType, prefix, suffix)) {
				number++;
				continue;
			}

			settingService.setLastUserNameNumberUsed(userType,number);
			username = paddedNumber;
			break;
		}
		if( username == null )
		{
			log.error("Max tries exhousted. Failed to generate a number-based username");
		}

		return username;
	}

	private String name23serial(Person person, String userType, String prefix, String suffix) {
		String personName = getName(person);
		String transliteratedName = Transliteration.transliterate(personName, null);
        String name = sanitize(transliteratedName);

        String nameTokens[] = name.split(" ");
        StringBuilder builder = new StringBuilder();
        
        // 2 letters from firstname
        for (int i = 0; i < 2; i++) {
        	if (nameTokens[0].length() > i) {
        		builder.append(nameTokens[0].charAt(i));
        	}
        	else {
        		builder.append("x");
        	}
        }
        
        // 3 letters from surname
        for (int i = 0; i < 3; i++) {
        	if (nameTokens.length > 1 && nameTokens[nameTokens.length - 1].length() > i) {
        		builder.append(nameTokens[nameTokens.length - 1].charAt(i));
        	}
        	else {
        		builder.append("x");
        	}
        }
        
        String infix = builder.toString();
        long serial = 1;

        // for now, we max out at 99, but we can safely increase this later if needed (se code below in serialToString)
        while (serial < 100) {
	        String candidate = infix + serialToString(serial);
	        
	        if (!isRejected(candidate, userType, prefix, suffix)) {
	            return candidate;
	        }
	        
	        serial++;
        }

		log.warn("Unable to generate serial for " + getName(person) + " / " + person.getUuid() + " maxed serial out");

        return null;
	}

	private String serialToString(long serial) {
		// we make sure we start at 01, 02, 03, but we skip to 100, 1000, etc later on
		if (serial < 10) {
			return "0" + serial;
		}

		if (serial < 100) {
			return "00" + serial;
		}

		if (serial < 1000) {
			return "000" + serial;
		}

		return "" + serial;
	}
	
	private String shortName(Person person, String userType, long len, String prefix, String suffix) {
		String personName = getName(person);
		String transliteratedName = Transliteration.transliterate(personName, null);
        String name = sanitize(transliteratedName);

        List<KeyValuePair> state = initialState(name, len);
        List<String> triedNames = new ArrayList<String>();

        int attempt = 0;
        do {
            String username = generate(state);
            if (username == null) {
            	log.warn("Failed to generate username for '" + name + "' from state on attempt " + attempt + ", skipping this try");
            }
            else {
	            if (!triedNames.contains(username)) {
	            	
	            	// fillers, not the best solution, but needed for very short names
	                while (username.length() < len) {
	                    username += "x";
	                }
	
	                if (!isRejected(username, userType, prefix, suffix)) {
	                    return username;
	                }
	
	                triedNames.add(username);
	            }
	
	            if (state.size() == 1) {
	                return null;
	            }
            }

            FuzzState(state, ++attempt, len);
        } while (attempt < 14);

        return null;
	}

	private String longName(Person person, String userType, String prefix, String suffix, boolean includeMiddleName) {
		String personName = getName(person);
		String transliteratedName = Transliteration.transliterate(personName, null);
        String[] splittedName = splitName(transliteratedName, includeMiddleName);

        String rootName = "";
        for (String split : splittedName)
		{
			if (rootName.length() > 0) {
				rootName += ".";
			}

			rootName += split;
		}


        if (!isRejected(rootName, userType, prefix, suffix)) {
        	return rootName;
        }

        return null;
	}

	private boolean isRejected(String word, String userType, String prefix, String suffix) {
		if (isSwearWord(word)) {
			log.info("Rejecting username '" + word + "' because it is a bad word");
			return true;
		}

		// checking against existing/old usernames requires prefix/suffix values to be added
		word = prefix + word + suffix;

		if (isExistingUsername(word, userType)) {
			log.info("Rejecting username '" + word + "' because it is already used by someone else");
			return true;
		}
		
		if (!configuration.getModules().getAccountCreation().isReuseExistingUsernames()) {
			if (isKnownUsername(word, userType)) {
				log.info("Rejecting username '" + word + "' because it has been used by someone else in the past");
				return true;
			}
		}
		
		return false;
	}

    private List<KeyValuePair> initialState(String input, long length) {
        String[] parts = input.split(" ");
        
        // this filters out empty blocks, because some people have multiple spaces between their nameparts *sigh*
        List<String> actualParts = new ArrayList<>();
        for (String part : parts) {
        	if (part.length() > 0) {
        		actualParts.add(part);
        	}
        }
        parts = actualParts.toArray(new String[0]);

        List<KeyValuePair> initialState = new ArrayList<KeyValuePair>();
        for (int i = 0; i < parts.length; i++) {
            if (i == 0 || i == (parts.length - 1)) {
                // firstname and surname are always important enough to get a "free" letter ;)
                initialState.add(new KeyValuePair(parts[i], 1));
            }
            else {
                initialState.add(new KeyValuePair(parts[i], 0));
            }
        }

        int index = 0;
        int badCount = 0; // just a way to keep track of bad tries, so we don't go into an infinite loop
        long count = length - ((parts.length >= 2) ? 2 : 1);
        while ((count--) > 0 && badCount < length) {
            KeyValuePair temp = initialState.get(index);

            if (temp.key.length() > temp.value) {
                initialState.get(index).value = temp.value + 1;
            }
            else {
                // try again
                badCount++;
                count++;
            }
            
            if (index == initialState.size() - 1) {
                index = 0;
            }
            else {
                index++;
            }
        }

        return initialState;
    }
    
    private String generate(List<KeyValuePair> state) {
        String finalUsername = "";
        for (KeyValuePair pair : state) {
        	// TODO: this can get out of bound - why? FIX THIS
        	if (pair.key.length() < pair.value) {
        		return null;
        	}

            finalUsername += pair.key.substring(0, (int) pair.value);
        }

        return finalUsername;
    }
	
	private String randomWord(long len) {
        String word = "";
        for (int i = 0; i < len; i++) {
            word += legalChars[random.nextInt(legalChars.length)];
        }

        return word;
	}
	
    private String removeIllegalCharacters(String word) {
        String newWord = "";

        for (char c : word.toLowerCase().toCharArray()) {
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                newWord += c;
            }
        }

        return newWord;
    }
    
    private String sanitize(String word) {
        String text = word.toLowerCase();
        text = text.replace("æ", "ae");
        text = text.replace("ø", "oe");
        text = text.replace("å", "aa");
        text = text.replaceAll("[^a-zA-Z0-9 ]*", "");

        return text.trim();
    }

    private String[] splitName(String fullName, boolean includeMiddleName) {
        String[] splittedName = fullName.toLowerCase()
        								.replace("æ", "ae")
										.replace("ø", "oe")
										.replace("å", "aa")
										.replace("-", "")
										.split(" ");

		List<String> newSplittedName = new ArrayList<>();
		int nameIdx = -1;

		// find first valid firstname and middlename
		for (int i = 0; i < splittedName.length; i++) {
			String name = removeIllegalCharacters(splittedName[i]);

			if (name.length() > 0) {
				newSplittedName.add(name);
				nameIdx = i;
			}
			if( (newSplittedName.size() == 1 & !includeMiddleName) || newSplittedName.size() > 1 ) {
				break;
			}
		}

		// find first valid surname if any split names are left
		for (int i = splittedName.length - 1; i > nameIdx; i--) {
			String name = removeIllegalCharacters(splittedName[i]);

			if (name.length() > 0) {
				newSplittedName.add(name);
				break;
			}
		}

        splittedName = newSplittedName.toArray(new String[0]);

        return splittedName;
    }

    private boolean isSwearWord(String word) {
    	if (badWordsService.findBadWord(word) != null) {
    		return true;
    	}

    	return false;
    }
    
    private boolean isKnownUsername(String word, String userType) {
    	if (knownUsernamesService.findByUsernameAndUserType(word, userType) != null) {
    		return true;
    	}

    	return false;
    }
    
    private boolean isExistingUsername(String word, String userType) {
    	if (SupportedUserTypeService.isExchange(userType)) {
    		word = word + "@%";

    		if (userService.findByUserIdLikeAndUserType(word, userType).size() > 0) {
    			return true;
    		}
    	}
    	else {
	    	if (userService.findByUserIdAndUserType(word, userType) != null) {
	    		return true;
	    	}
    	}
    	
    	return false;
    }

    private void FuzzState(List<KeyValuePair> state, int attempt, long length) {
        long left = length;

        switch (attempt) {
            case 1:
				for (int i = 0; i < state.size(); i++) {
					long value = 0;

					if (i == 0) {
						value = length / 3 + ((length % 2 == 1) ? 1 : 0);
					}
					else if (i == 1 && state.size() >= 3) {
						value = 1;
					}
					else if (i == 2 && state.size() >= 4) {
						value = 1;
					}
					else if (i == (state.size() - 1)) {
						value = length / 3;
					}

					if (value > left) {
						value = left;
					}
					left -= value;

					state.get(i).value = value;
				}

				if (left > 0) {
					fillFromLeft(state, left);
				}
                break;
            case 2:
				for (int i = 0; i < state.size(); i++) {
					long value = 0;

					if (i == 0) {
						value = length / 3;
					}
					else if (i == 1 && state.size() >= 3) {
						value = 1;
					}
					else if (i == 2 && state.size() >= 4) {
						value = 1;
					}
					else if (i == (state.size() - 1)) {
						value = length / 3 + ((length % 2 == 1) ? 2 : 1);
					}

					if (value > left) {
						value = left;
					}
					left -= value;

					state.get(i).value = value;
				}

				if (left > 0) {
					fillFromRight(state, left);
				}
                break;
            case 3:
				for (int i = 0; i < state.size(); i++) {
					long value = 0;

					if (i == 0) {
						value = length / 3 + ((length % 2 == 1) ? 2 : 1);
					}
					else if (i == 2 && state.size() >= 4) {
						value = 1;
					}
					else if (i == 3 && state.size() >= 5) {
						value = 1;
					}
					else if (i == (state.size() - 1)) {
						value = length / 3;
					}

					if (value > left) {
						value = left;
					}
					left -= value;

					state.get(i).value = value;
				}

				if (left > 0) {
					fillFromLeft(state, left);
				}
				break;
            case 4:
				for (int i = 0; i < state.size(); i++) {
					long value = 0;

					if (i == 0) {
						value = length / 3;
					}
					else if (i == 2 && state.size() >= 4) {
						value = 1;
					}
					else if (i == 3 && state.size() >= 5) {
						value = 1;
					}
					else if (i == (state.size() - 1)) {
						value = length / 3 + ((length % 2 == 1) ? 2 : 1);
					}

					if (value > left) {
						value = left;
					}
					left -= value;

					state.get(i).value = value;
				}

				if (left > 0) {
					fillFromRight(state, left);
				}
				break;
            case 5:
				for (int i = 0; i < state.size(); i++) {
					long value;

					if (i == 0) {
						value = length / 4 + ((length % 2 == 1) ? 2 : 1);
					}
					else if (i == (state.size() - 1)) {
						value = length / 4;
					}
					else {
						value = 1;
					}

					if (value > left) {
						value = left;
					}
					left -= value;

					state.get(i).value = value;
				}

				if (left > 0) {
					fillFromLeft(state, left);
				}
                break;
            case 6:
				for (int i = 0; i < state.size(); i++) {
					long value;

					if (i == 0) {
						value = length / 4;
					}
					else if (i == (state.size() - 1)) {
						value = length / 4 + ((length % 2 == 1) ? 2 : 1);
					}
					else {
						value = 1;
					}

					if (value > left) {
						value = left;
					}
					left -= value;

					state.get(i).value = value;
				}

				if (left > 0) {
					fillFromRight(state, left);
				}
                break;
            case 7:
				for (int i = 0; i < state.size(); i++) {
					long value = 0;

					if (i == 0) {
						value = length / 2 + ((length % 2 == 1) ? 2 : 1);
					}
					else if (i == (state.size() - 1)) {
						value = length / 2;
					}

					if (value > left) {
						value = left;
					}
					left -= value;

					state.get(i).value = value;
				}

				if (left > 0) {
					fillFromLeft(state, left);
				}
                break;
            case 8:
				for (int i = 0; i < state.size(); i++) {
					long value = 0;

					if (i == 0) {
						value = length / 2;
					}
					else if (i == (state.size() - 1)) {
						value = length / 2 + ((length % 2 == 1) ? 2 : 1);
					}

					if (value > left) {
						value = left;
					}
					left -= value;

					state.get(i).value = value;
				}

				if (left > 0) {
					fillFromRight(state, left);
				}
                break;
            case 9:
                for (int i = 0; i < state.size(); i++) {
                    long value;

                    if (i == 0) {
                        value = length / 4;
                    }
                    else if (i == (state.size() - 1)) {
                        value = length / 4;
                    }
                    else {
                        value = 2;
                    }

                    if (value > left) {
                        value = left;
                    }
                    left -= value;

                    state.get(i).value = value;
                }

                if (left > 0) {
                    fillFromMiddle(state, left);
                }
                break;
            case 10:
                for (int i = 0; i < state.size(); i++) {
                    long value;

                    if (i == 0) {
                        value = length / 4;
                    }
                    else if (i == (state.size() - 1)) {
                        value = length / 4;
                    }
                    else {
                        value = 2;
                    }

                    if (value > left) {
                        value = left;
                    }
                    left -= value;

                    state.get(i).value = value;
                }

                if (left > 0) {
                    fillFromMiddle(state, left);
                }
                break;
            case 11:
                for (int i = 0; i < state.size(); i++) {
                    long value = 0;

                    if (i == 0) {
                        value = length / 4 + ((length % 2 == 1) ? 1 : 0);
                    }
                    else if (i == 1 && state.size() >= 3) {
                        value = 1;
                    }
                    else if (i == 2 && state.size() >= 4) {
                        value = 1;
                    }
                    else if (i == (state.size() - 1)) {
                        value = length / 3 + 2;
                    }

                    if (value > left) {
                        value = left;
                    }
                    left -= value;

                    state.get(i).value = value;
                }

                if (left > 0) {
                    fillFromLeft(state, left);
                }
                break;
            case 12:
                for (int i = 0; i < state.size(); i++) {
                    long value = 0;

					if (i == 0) {
						value = length / 2 + 2;
					}
					else if (i == 1 && state.size() >= 3) {
						value = 1;
					}
					else if (i == 2 && state.size() >= 4) {
						value = 1;
					}
					else if (i == (state.size() - 1)) {
						value = length / 4 + ((length % 2 == 1) ? 1 : 0);
					}

					if (value > left) {
						value = left;
					}
					left -= value;

                    state.get(i).value = value;
                }

				if (left > 0) {
					fillFromRight(state, left);
				}
                break;
            case 13:
				for (int i = 0; i < state.size(); i++) {
					long value = 0;

					if (i == 0) {
						value = length / 3 + ((length % 2 == 1) ? 2 : 1);
					}
					else if (i == 2 && state.size() >= 4) {
						value = 2;
					}
					else if (i == 3 && state.size() >= 5) {
						value = 2;
					}
					else if (i == (state.size() - 1)) {
						value = length / 3;
					}

					if (value > left) {
						value = left;
					}
					left -= value;

					state.get(i).value = value;
				}

				if (left > 0) {
					fillFromLeft(state, left);
				}
                break;
            default:
            	log.warn("Unknown fuzzstate: " + attempt);
                break;
        }
    }

    private void fillFromMiddle(List<KeyValuePair> state, long left) {
        int i = 1;

        do {
        	state.get(i).value = state.get(i).value + 1;

            if ((++i) >= state.size()) {
                i = 0;
            }
        } while ((--left) > 0);
    }

    private void fillFromLeft(List<KeyValuePair> state, long left) {
        int i = 0;

        do {
        	state.get(i).value = state.get(i).value + 1;

            if ((++i) >= state.size())
            {
                i = 0;
            }
        } while ((--left) > 0);
    }

    private void fillFromRight(List<KeyValuePair> state, long left) {
        int i = state.size() - 1;

        do
        {
        	state.get(i).value = state.get(i).value + 1;

            if ((--i) < 0)
            {
                i = state.size() - 1;
            }
        } while ((--left) > 0);
    }
    
    class KeyValuePair {
    	String key;
    	long value;
    	
    	KeyValuePair(String key, long value) {
    		this.key = key;
    		this.value = value;
    	}
    }

	private String getName(Person person) {
		return configuration.getModules().getAccountCreation().isUseCprNameForUsernameGenerator() ? PersonService.getCprName(person) : PersonService.getName(person);
	}
}
