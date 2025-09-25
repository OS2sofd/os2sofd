package dk.digitalidentity.sofd.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.dao.ReservedUsernameDao;
import dk.digitalidentity.sofd.dao.model.AccountOrder;
import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.Notification;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.ReservedUsername;
import dk.digitalidentity.sofd.dao.model.SupportedUserType;
import dk.digitalidentity.sofd.dao.model.User;
import dk.digitalidentity.sofd.dao.model.enums.AccountOrderStatus;
import dk.digitalidentity.sofd.dao.model.enums.AccountOrderType;
import dk.digitalidentity.sofd.dao.model.enums.EntityType;
import dk.digitalidentity.sofd.dao.model.enums.NotificationType;
import dk.digitalidentity.sofd.dao.model.enums.PhoneType;
import dk.digitalidentity.sofd.service.opus.dto.Envelope;
import dk.digitalidentity.sofd.service.opus.dto.Kommunikation;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class OpusService {

	/* sample payload for testing
	 * 
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:urn="urn:oio:medarbejder:1.0.0" xmlns:urn1="urn:oio:sagdok:3.0.0">
<soapenv:Header/>
<soapenv:Body>
  <urn:LaesInput>
      <urn1:ModtagerRef>
        <urn1:UUIDIdentifikator>NotUsed</urn1:UUIDIdentifikator>
        <urn1:URNIdentifikator>urn:oio:kmd:lpe:modtager:{0}</urn1:URNIdentifikator>
      </urn1:ModtagerRef>
      <urn1:MedarbejderRef>
        <urn1:UUIDIdentifikator>NotUsed</urn1:UUIDIdentifikator>
        <urn1:URNIdentifikator>urn:oio:kmd:lpe:medarbejdernummer:{0}</urn1:URNIdentifikator>
      </urn1:MedarbejderRef>
    <urn:Datakategori>0105</urn:Datakategori>
  </urn:LaesInput>
</soapenv:Body>
</soapenv:Envelope>
	 * 
	 */
	private static final String OPUS_SOAP_BEGIN = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:urn=\"urn:oio:medarbejder:1.0.0\" xmlns:urn1=\"urn:oio:sagdok:3.0.0\"><soapenv:Header/><soapenv:Body>";
	private static final String OPUS_SOAP_END = "</soapenv:Body></soapenv:Envelope>";

	private static final String OPUS_SOAP_RET_BEGIN = "<urn:RetInput>";
	private static final String OPUS_SOAP_RET_END = "</urn:RetInput>";

	private static final String OPUS_SOAP_LAES_BEGIN = "<urn:LaesInput>";
	private static final String OPUS_SOAP_LAES_END = "</urn:LaesInput>";

	private static final String OPUS_SOAP_DATAKATAGORI = "<urn:Datakategori>0105</urn:Datakategori>";
	
	// {0} : ID'et på kommunens organisation (tenant i KMD OPUS)
	private static final String OPUS_SOAP_MODTAGER_REF = "<urn1:ModtagerRef><urn1:UUIDIdentifikator>NotUsed</urn1:UUIDIdentifikator><urn1:URNIdentifikator>urn:oio:kmd:lpe:modtager:{0}</urn1:URNIdentifikator></urn1:ModtagerRef>";

	// {0} : ID'et på medarbejderen (medarbejdernummer fra ansættelsen)
	private static final String OPUS_SOAP_MEDARBEJDER_REF = "<urn1:MedarbejderRef><urn1:UUIDIdentifikator>NotUsed</urn1:UUIDIdentifikator><urn1:URNIdentifikator>urn:oio:kmd:lpe:medarbejdernummer:{0}</urn1:URNIdentifikator></urn1:MedarbejderRef>";

	private static final String OPUS_SOAP_ATTRIBUTLISTE_BEGIN = "<urn:AttributListe><urn:Egenskaber>";
	private static final String OPUS_SOAP_ATTRIBUTLISTE_END = "</urn:Egenskaber></urn:AttributListe>";
	
	// {0} = 2020-10-23
	// {1} = 9999-12-31
	// {2} = 0010 (email) / 0001 (brugerid) / (9905) IT Bruger / 0020 (telefonnummer eller mobilnummer) / 9021 (afdelingsnummer) 
	// {3} = Værdi på ovenstående (fx email eller brugernavn)
	private static final String OPUS_SOAP_KOMMUNIKATION = "<urn:Kommunikation><urn:Gyldighedsstart>{0}</urn:Gyldighedsstart><urn:Gyldighedsstop>{1}</urn:Gyldighedsstop><urn:Sekvensnummer>000</urn:Sekvensnummer><urn:Kommunikationsart>{2}</urn:Kommunikationsart><urn:KommunikationsID>{3}</urn:KommunikationsID></urn:Kommunikation>";
	
	@Qualifier("opusRestTemplate")
	@Autowired
	private RestTemplate restTemplate;
	
	@Autowired
	private SofdConfiguration configuration;

	@Autowired
	private PersonService personService;

	@Autowired
	private AccountOrderService accountOrderService;
	
	@Autowired
	private ReservedUsernameDao reservedUsernameDao;
	
	@Autowired
	private NotificationService notificationService;
	
	@Autowired
	private SupportedUserTypeService supportedUserTypeService;

	@Transactional(rollbackFor = Exception.class)
	public void handleOrders() {
		handleCreate();
		handleDelete();
	}

	public void updateEmailForAllPersons() {
		log.info("Updating Opus email for all persons");
		var persons = personService.getActive();
		log.info("Found " + persons.size() + " persons");
		var counter = 0;
		for( var person : persons) {
			updateEmail(person);
			counter++;
			if( counter % 100 == 0) {
				log.info("Updated " + counter + " of " + persons.size() + " persons");
			}
		}
		log.info("Done updating Opus emails");
	}

	public void updateEmail(Person person) {
		List<Affiliation> opusAffiliations = AffiliationService.notStoppedAffiliations(person.getAffiliations().stream().filter(a -> Objects.equals(a.getMaster(), "OPUS")).collect(Collectors.toList()));
		if (opusAffiliations.size() == 0) {
			return;
		}

		for (Affiliation affiliation : opusAffiliations) {
			ExistingOpusValue existingOpusValue = callOpusAndReadUser(affiliation.getEmployeeId());

			if (!StringUtils.hasLength(existingOpusValue.itBruger.value)) {
				log.info("Unable to update email address for " + person.getUuid() + " / " + affiliation.getEmployeeId() + " due to no OPUS account");
				continue;
			}
			
			String email = getEmail(person, affiliation.getEmployeeId());
			
			callOpusAndUpdateEmail(existingOpusValue, email, affiliation.getEmployeeId(), affiliation.getStopDate());
		}
	}
	
	public void updatePhones(Person person) {
		List<Affiliation> opusAffiliations = AffiliationService.notStoppedAffiliations(person.getAffiliations().stream().filter(a -> Objects.equals(a.getMaster(), "OPUS")).collect(Collectors.toList()));
		if (opusAffiliations.isEmpty()) {
			return;
		}

		for (Affiliation affiliation : opusAffiliations) {
			ExistingOpusValue existingOpusValue = callOpusAndReadUser(affiliation.getEmployeeId());

			if (!StringUtils.hasLength(existingOpusValue.itBruger.value)) {
				log.info("Unable to update phones for " + person.getUuid() + " / " + affiliation.getEmployeeId() + " due to no OPUS account");
				continue;
			}
			
			String phone = getPhone(person);
			String departmentNumber = getDepartmentPhone(person);
			
			callOpusAndUpdatePhones(existingOpusValue, phone, departmentNumber, affiliation.getEmployeeId(), affiliation.getStopDate());
		}
	}

	public void updateStopDate(Person person, String employeeId) {
		Optional<User> opusUser = PersonService.getUsers(person).stream().filter(u -> Objects.equals(u.getEmployeeId(), employeeId) && u.getUserType().equals(SupportedUserTypeService.getOpusUserType())).findFirst();

		if (opusUser.isPresent()) {
			User user = opusUser.get();
			
			Date stopDate = null;
			Optional<Affiliation> oAffiliation = person.getAffiliations().stream().filter(a -> "OPUS".equals(a.getMaster()) && Objects.equals(a.getEmployeeId(), user.getEmployeeId())).findFirst();
			if (oAffiliation.isPresent()) {
				stopDate = oAffiliation.get().getStopDate();
			}

			ExistingOpusValue existingOpusValue = callOpusAndReadUser(user.getEmployeeId());
			if (!StringUtils.hasLength(existingOpusValue.itBruger.value) || !StringUtils.hasLength(existingOpusValue.userId.value)) {
				log.warn("Unable to update stopDate for " + person.getUuid() + " / " + user.getEmployeeId() + " due to no OPUS account, but we have one in SOFD: " + user.getUserId());
				return;
			}
			
			callOpusAndUpdateStopDate(employeeId, existingOpusValue, stopDate);
		}
	}

	private void failAndNotify(Person person, AccountOrder accountOrder, String message) {
		if (person != null) {
			String prettyName = supportedUserTypeService.getPrettyName(accountOrder.getUserType());
			
			Notification notification = new Notification();
			notification.setActive(true);
			notification.setAffectedEntityName(PersonService.getName(person));
			notification.setAffectedEntityType(EntityType.PERSON);
			notification.setAffectedEntityUuid(accountOrder.getPersonUuid());
			notification.setMessage(prettyName + ": " + message);
			notification.setNotificationType(NotificationType.ACCOUNT_ORDER_FAILURE);
			notification.setCreated(new Date());
			notificationService.save(notification);
		}
		
		accountOrder.setStatus(AccountOrderStatus.FAILED);
		accountOrder.setMessage(message);
		accountOrderService.save(accountOrder);
	}

	private void handleCreate() {
		List<AccountOrder> pendingOrders = accountOrderService.getPendingOrders(SupportedUserTypeService.getOpusUserType(), AccountOrderType.CREATE);
		
		pendingOrders = accountOrderService.identifyAndDeleteDuplicates(pendingOrders);
		if (pendingOrders.size() == 0) {
			return;
		}
		
		log.info("Processing " + pendingOrders.size() + " create orders");

		for (AccountOrder order : pendingOrders) {
			String personUuid = order.getPersonUuid();
			Person person = personService.getByUuid(personUuid);
			if (person == null) {
				log.error("Failed to process OPUS order " + order.getId() + ", because person with UUID " + personUuid + " does not exist!");
				
				failAndNotify(null, order, "Person eksisterer ikke længere i SOFD");
				
				continue;
			}
			
			String employeeId = order.getEmployeeId();
			String requestedUserId = order.getRequestedUserId();
			String email = getEmail(person, employeeId);
			String phone = getPhone(person);
			String departmentNumber = getDepartmentPhone(person);
			Date startDate = null;
			Date stopDate = null;

			// an order without an employeeId does not make sense
			if (!StringUtils.hasLength(employeeId)) {
				failAndNotify(person, order, "Ordren er ikke knyttet til noget tilhørsforhold");
				continue;
			}
			
			Affiliation affiliation = person.getAffiliations().stream().filter(a -> a.getMaster().equals("OPUS") && Objects.equals(a.getEmployeeId(), employeeId)).findAny().orElse(null);
			if (affiliation == null) {
				failAndNotify(person, order, "Personen har ikke noget tilhørsforhold");
				continue;
			}
			
			LocalDateTime now = LocalDateTime.now();
			LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);
			LocalDateTime affiliationStartDate = convertToLocalDateTime(affiliation.getStartDate());

			if (affiliationStartDate.isBefore(threeMonthsAgo)) {
				startDate = convertToDate(threeMonthsAgo);
			}
			else if (affiliationStartDate.isBefore(now)) {
				startDate = convertToDate(affiliationStartDate);
			}
			else {
				startDate = new Date();
			}			

			ExistingOpusValue existingOpusValue = callOpusAndReadUser(employeeId);
			if (StringUtils.hasLength(existingOpusValue.itBruger.value) && StringUtils.hasLength(existingOpusValue.userId.value)) {
				if( StringUtils.hasLength(existingOpusValue.itBruger.stopDate) && LocalDate.parse(existingOpusValue.itBruger.stopDate).isBefore(LocalDate.now()) ) {
					// the user already exists, but stopdate is passed, so we call opus and reactivate it
					requestedUserId = existingOpusValue.userId.value;
				}
				else {
					// person is null on purpose, as no notification is then generated
					failAndNotify(null, order, "Personen har allerede en OPUS konto for medarbejderID " + employeeId + ": " + existingOpusValue.userId.value);
					continue;
				}
			}

			OpusStatusCodeWrapper status = callOpusAndOrderUser(existingOpusValue, employeeId, requestedUserId, email, startDate, stopDate, phone, departmentNumber);

			if (status.status.equals(OpusStatusCode.OK)) {
				order.setActualUserId(requestedUserId);
				order.setStatus(AccountOrderStatus.CREATED);
				accountOrderService.save(order);

				// notify relevant parties about success
				accountOrderService.notify(order);
			}
			else if (status.status.equals(OpusStatusCode.NETWORK_ERROR)) {
				; // try again later
			}
			else {
				failAndNotify(person, order, status.message);
			}
		}
		
		log.info("Done processing create orders");
	}
	
	private void handleDelete() {
		List<AccountOrder> pendingDeactivateOrders = accountOrderService.getPendingOrders(SupportedUserTypeService.getOpusUserType(), AccountOrderType.DEACTIVATE);
		List<AccountOrder> pendingDeleteOrders = accountOrderService.getPendingOrders(SupportedUserTypeService.getOpusUserType(), AccountOrderType.DELETE);

		String prettyName = supportedUserTypeService.getPrettyName(SupportedUserTypeService.getOpusUserType());
		
		// change deactivate to delete (as we only handle delete orders for OPUS)
		pendingDeactivateOrders.stream().forEach(o -> o.setOrderType(AccountOrderType.DELETE));

		// filter duplicates
		List<AccountOrder> pendingOrders = new ArrayList<>();
		pendingOrders.addAll(pendingDeactivateOrders);
		pendingOrders.addAll(pendingDeleteOrders);
		pendingOrders = accountOrderService.identifyAndDeleteDuplicates(pendingOrders);

		if (pendingOrders.size() == 0) {
			return;
		}

		log.info("Processing " + pendingOrders.size() + " delete orders");
		
		for (AccountOrder order : pendingOrders) {
			String personUuid = order.getPersonUuid();
			Person person = personService.getByUuid(personUuid);
			if (person == null) {
				log.error("Failed to process OPUS order " + order.getId() + ", because person with UUID " + personUuid + " does not exist!");
				order.setStatus(AccountOrderStatus.FAILED);
				order.setMessage("Person does not exist!");
				accountOrderService.save(order);
				
				continue;
			}
			
			String employeeId = order.getEmployeeId();			
			String userId = order.getRequestedUserId();

			ExistingOpusValue existingOpusValue = callOpusAndReadUser(employeeId);
			OpusStatusCodeWrapper status = callOpusAndDeleteUser(employeeId, userId, existingOpusValue);

			if (status.status.equals(OpusStatusCode.OK)) {
				order.setActualUserId(userId);
				order.setStatus(AccountOrderStatus.DELETED);
				accountOrderService.save(order);

				// notify relevant parties about success
				accountOrderService.notify(order);
			}
			else if (status.status.equals(OpusStatusCode.NETWORK_ERROR)) {
				; // try again later
			}
			else {
				order.setStatus(AccountOrderStatus.FAILED);
				order.setMessage(status.message);
				accountOrderService.save(order);
				
				// notify
				Notification notification = new Notification();
				notification.setActive(true);
				notification.setAffectedEntityName(PersonService.getName(person));
				notification.setAffectedEntityType(EntityType.PERSON);
				notification.setAffectedEntityUuid(person.getUuid());
				
				notification.setMessage(prettyName + ": " + status.message);
				notification.setNotificationType(NotificationType.ACCOUNT_ORDER_FAILURE);
				notification.setCreated(new Date());

				notificationService.save(notification);
			}
		}
		
		log.info("Done processing delete orders");
	}
	
	private String getEmail(Person person, String employeeId) {
		SupportedUserType exchangeUserType = supportedUserTypeService.findByKey(SupportedUserTypeService.getExchangeUserType());

		String email = null;
		if (person != null) {
			email = PersonService.getEmail(person);
		}

		if (email == null) {
			String defaultEmail = configuration.getModules().getAccountCreation().getOpusHandler().getDefaultEmail();
			if (StringUtils.hasLength(defaultEmail)) {
				email = defaultEmail;
				
				// special case, where the standard email is personalized with the employeeId
				if (email.startsWith("@")) {
					email = employeeId + email;
				}
			}
			else if (person != null) {
				if (exchangeUserType.isSingleUserMode()) {
					ReservedUsername reservedUsername = reservedUsernameDao.findByPersonUuidAndUserType(person.getUuid(), SupportedUserTypeService.getExchangeUserType());
					if (reservedUsername != null) {
						email = reservedUsername.getUserId();
					}
				}
				else {
					ReservedUsername reservedUsername = reservedUsernameDao.findByPersonUuidAndEmployeeIdAndUserType(person.getUuid(), employeeId, SupportedUserTypeService.getExchangeUserType());
					if (reservedUsername != null) {
						email = reservedUsername.getUserId();
					}
				}
				
				// if we get a reserved username, it does not contain a domain name, so we need to add that
				if (email != null) {
					if (StringUtils.hasLength(configuration.getModules().getAccountCreation().getOpusHandler().getDefaultEmailDomain())) {
						email += configuration.getModules().getAccountCreation().getOpusHandler().getDefaultEmailDomain();
					}
					else {
						// sorry, we have to null it :(
						email = null;
					}
				}
			}
		}
		
		if (email == null) {
			email = "no-email@kommune.dk";
		}
		
		if (configuration.getModules().getAccountCreation().getOpusHandler().isEmailInUpperCase()) {
			email = email.toUpperCase();
		}
		
		return email;
	}
	
	class OpusStatusCodeWrapper {
		OpusStatusCode status;
		String message;
	}

	private enum OpusStatusCode {
		OK,
		NETWORK_ERROR,
		FAILURE
	};

	static class ExistingField {
		String value;
		String startDate;
		String stopDate;
	}
	
	static class ExistingOpusValue {
		ExistingField email;
		ExistingField userId;
		ExistingField itBruger;
		ExistingField afdelingsnummer;
		ExistingField direkteTelefonnummer;
	}

	private ExistingOpusValue callOpusAndReadUser(String employeeId) {
		ExistingOpusValue opusResponse = new ExistingOpusValue();
		opusResponse.email = new ExistingField();
		opusResponse.userId = new ExistingField();
		opusResponse.itBruger = new ExistingField();
		opusResponse.afdelingsnummer = new ExistingField();
		opusResponse.direkteTelefonnummer = new ExistingField();

		StringBuilder builder = new StringBuilder();

		builder.append(OPUS_SOAP_BEGIN);
		builder.append(OPUS_SOAP_LAES_BEGIN);
		
		builder.append(OPUS_SOAP_MODTAGER_REF.replace("{0}", configuration.getModules().getAccountCreation().getOpusHandler().getMunicipalityNumber()));
		builder.append(OPUS_SOAP_MEDARBEJDER_REF.replace("{0}", employeeId));
		
		builder.append(OPUS_SOAP_DATAKATAGORI);

		builder.append(OPUS_SOAP_LAES_END);
		builder.append(OPUS_SOAP_END);
				
    	HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "text/xml; charset=utf-8");
        headers.add("SOAPAction", "http://sap.com/xi/WebService/soap1.1");

		String payload = builder.toString();
    	HttpEntity<String> request = new HttpEntity<String>(payload, headers);
		ResponseEntity<String> response;

    	// KMD has some issues, so we might have to try multiple times
    	int tries = 3;
    	do {
    		response = restTemplate.postForEntity(configuration.getModules().getAccountCreation().getOpusHandler().getConvertedUrl(), request, String.class);
			if (response.getStatusCodeValue() != 200) {
				if (--tries >= 0) {
					log.warn("Laes Request - Got responseCode " + response.getStatusCodeValue() + " from service: " + response.getBody());
					
					try {
						Thread.sleep(5000);
					}
					catch (InterruptedException ex) {
						;
					}
				}
				else {
					log.error("Laes Request - Got responseCode " + response.getStatusCodeValue() + " from service: " + response.getBody());
					
					return opusResponse;
				}
			}
			else {
				break;
			}
    	} while (true);

		String responseBody = response.getBody();

		try {
			XmlMapper xmlMapper = XmlMapper.builder().configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true).build();

			Envelope envelope = xmlMapper.readValue(responseBody, Envelope.class);
			
			if (envelope.getBody() != null) {
				if (envelope.getBody().getLaesOutput() != null) {
					if (envelope.getBody().getLaesOutput().getStandardRetur() != null) {
						if (envelope.getBody().getLaesOutput().getStandardRetur().getStatusKode() != 0) {
							log.warn("Got error message from OPUS laes operation: " + envelope.getBody().getLaesOutput().getStandardRetur().getStatusKode() + " / " + envelope.getBody().getLaesOutput().getStandardRetur().getFejlbeskedTekst());
							
							return opusResponse;
						}
					}
					else {
						log.error("Got response without status on OPUS laes operation:\n" + responseBody);
						
						return opusResponse;
					}
				}
			}
			else {
				log.error("Got response without body on OPUS laes operation:\n" + responseBody);

				return opusResponse;
			}

			for (Kommunikation k : envelope.getBody().getLaesOutput().getLaesResultat().getRegistrering().getAttributListe().getEgenskaber().getKommunikation()) {
				if ("0001".equals(k.getKommunikationsart())) {
					opusResponse.userId.startDate = k.getGyldighedsstart();
					opusResponse.userId.stopDate = k.getGyldighedsstop();
					opusResponse.userId.value = k.getKommunikationsID();
				}
				else if ("0010".equals(k.getKommunikationsart())) {
					opusResponse.email.startDate = k.getGyldighedsstart();
					opusResponse.email.stopDate = k.getGyldighedsstop();
					opusResponse.email.value = k.getKommunikationsID();
				}
				else if ("9905".equals(k.getKommunikationsart())) {
					opusResponse.itBruger.startDate = k.getGyldighedsstart();
					opusResponse.itBruger.stopDate = k.getGyldighedsstop();
					opusResponse.itBruger.value = k.getKommunikationsID();
				}
				else if ("0020".equals(k.getKommunikationsart())) {
					opusResponse.direkteTelefonnummer.startDate = k.getGyldighedsstart();
					opusResponse.direkteTelefonnummer.stopDate = k.getGyldighedsstop();
					opusResponse.direkteTelefonnummer.value = k.getKommunikationsID();
				}
				else if ("9021".equals(k.getKommunikationsart())) {
					opusResponse.afdelingsnummer.startDate = k.getGyldighedsstart();
					opusResponse.afdelingsnummer.stopDate = k.getGyldighedsstop();
					opusResponse.afdelingsnummer.value = k.getKommunikationsID();
				}
			}
			
			return opusResponse;
		}
		catch (Exception ex) {
			log.error("Laes Request - Failed to decode response: " + responseBody, ex);
			
			return opusResponse;
		}
	}
	
	// TODO: we should probably perform this using our "one call per field" logic - though to be honest this method is practically
	//       never called, as we do not delete OPUS users rather we let OPUS do it by itself
	private OpusStatusCodeWrapper callOpusAndDeleteUser(String employeeId, String userId, ExistingOpusValue existingOpusValue) {
		StringBuilder builder = new StringBuilder();

		builder.append(OPUS_SOAP_BEGIN);
		builder.append(OPUS_SOAP_RET_BEGIN);
		
		builder.append(OPUS_SOAP_MODTAGER_REF.replace("{0}", configuration.getModules().getAccountCreation().getOpusHandler().getMunicipalityNumber()));
		builder.append(OPUS_SOAP_MEDARBEJDER_REF.replace("{0}", employeeId));
		
		builder.append(OPUS_SOAP_ATTRIBUTLISTE_BEGIN);

		builder.append(OPUS_SOAP_KOMMUNIKATION
				.replace("{0}", (existingOpusValue.userId.startDate != null) ? existingOpusValue.userId.startDate : LocalDate.now().minusDays(1).toString())
				.replace("{1}", LocalDate.now().toString())
				.replace("{2}", "0001")
				.replace("{3}", (existingOpusValue.userId.value != null) ? existingOpusValue.userId.value : userId));
		
		builder.append(OPUS_SOAP_KOMMUNIKATION
				.replace("{0}", (existingOpusValue.email.startDate != null) ? existingOpusValue.email.startDate : LocalDate.now().minusDays(1).toString())
				.replace("{1}", LocalDate.now().toString())
				.replace("{2}", "0010")
				.replace("{3}", (existingOpusValue.email.value != null) ? existingOpusValue.email.value : getEmail(null, null)));
		
		builder.append(OPUS_SOAP_KOMMUNIKATION
				.replace("{0}", (existingOpusValue.itBruger.startDate != null) ? existingOpusValue.itBruger.startDate : LocalDate.now().minusDays(1).toString())
				.replace("{1}", LocalDate.now().toString())
				.replace("{2}", "9905")
				.replace("{3}", (existingOpusValue.itBruger.value != null) ? existingOpusValue.itBruger.value : "IT Bruger"));
		
		if (existingOpusValue.direkteTelefonnummer != null && StringUtils.hasLength(existingOpusValue.direkteTelefonnummer.value)) {
			builder.append(OPUS_SOAP_KOMMUNIKATION
				.replace("{0}", (existingOpusValue.direkteTelefonnummer.startDate != null) ? existingOpusValue.direkteTelefonnummer.startDate : LocalDate.now().minusDays(1).toString())
				.replace("{1}", LocalDate.now().toString())
				.replace("{2}", "0020")
				.replace("{3}", existingOpusValue.direkteTelefonnummer.value));
		}

		if (existingOpusValue.afdelingsnummer != null && StringUtils.hasLength(existingOpusValue.afdelingsnummer.value)) {
			builder.append(OPUS_SOAP_KOMMUNIKATION
				.replace("{0}", (existingOpusValue.afdelingsnummer.startDate != null) ? existingOpusValue.afdelingsnummer.startDate : LocalDate.now().minusDays(1).toString())
				.replace("{1}", LocalDate.now().toString())
				.replace("{2}", "9021")
				.replace("{3}", existingOpusValue.afdelingsnummer.value));
		}

		builder.append(OPUS_SOAP_ATTRIBUTLISTE_END);

		builder.append(OPUS_SOAP_RET_END);
		builder.append(OPUS_SOAP_END);
		String payload = builder.toString();

		return callOpusRet(payload);
	}
	
	private void callOpusAndUpdateStopDate(String employeeId, ExistingOpusValue existingOpusValue, Date stopDate) {
		String stopDateStr = "9999-12-31";
		if (stopDate != null) {
			 LocalDate tts = Instant.ofEpochMilli(stopDate.getTime())
		    	      .atZone(ZoneId.systemDefault())
		    	      .toLocalDate();
			 
			 stopDateStr = tts.toString();
		}

		log.info("Updating " + employeeId + " stopDate to " + stopDateStr);
		
		String startEmailVal = existingOpusValue.email.startDate != null ? existingOpusValue.email.startDate : LocalDate.now().toString();
		String emailVal = existingOpusValue.email.value != null ? existingOpusValue.email.value : getEmail(null, null);
		OpusStatusCodeWrapper emailStatus = updateValue(employeeId, emailVal, "0010", startEmailVal, stopDateStr);
		if (!emailStatus.status.equals(OpusStatusCode.OK)) {
			log.warn("Failed to update " + employeeId + " email to " + emailVal + " when updating stopDate. Message: " + emailStatus.message);
		}
		
		String startUserIdVal = existingOpusValue.userId.startDate != null ? existingOpusValue.userId.startDate : LocalDate.now().toString();
		OpusStatusCodeWrapper userIdStatus = updateValue(employeeId, existingOpusValue.userId.value, "0001", startUserIdVal, stopDateStr);
		if (!userIdStatus.status.equals(OpusStatusCode.OK)) {
			log.warn("Failed to update " + employeeId + " userId to " + existingOpusValue.userId.value + " when updating stopDate. Message: " + userIdStatus.message);
		}
		
		String startItBrugerVal = existingOpusValue.itBruger.startDate != null ? existingOpusValue.itBruger.startDate : LocalDate.now().toString();
		OpusStatusCodeWrapper itBrugerStatus = updateValue(employeeId, "IT Bruger", "9905", startItBrugerVal, stopDateStr);
		if (!itBrugerStatus.status.equals(OpusStatusCode.OK)) {
			log.warn("Failed to update " + employeeId + " IT Bruger to IT Bruger when updating stopDate. Message: " + itBrugerStatus.message);
		}
		
		if (existingOpusValue.direkteTelefonnummer != null && StringUtils.hasLength(existingOpusValue.direkteTelefonnummer.value)) {
			String startVal = existingOpusValue.direkteTelefonnummer.startDate != null ? existingOpusValue.direkteTelefonnummer.startDate : LocalDate.now().toString();
			OpusStatusCodeWrapper status = updateValue(employeeId, existingOpusValue.direkteTelefonnummer.value, "0020", startVal, stopDateStr);
			if (!status.status.equals(OpusStatusCode.OK)) {
				log.warn("Failed to update " + employeeId + " direkteTelefonnummer to " + existingOpusValue.direkteTelefonnummer.value + " when updating stopDate. Message: " + status.message);
			}
		}
		
		if (existingOpusValue.afdelingsnummer != null && StringUtils.hasLength(existingOpusValue.afdelingsnummer.value)) {
			String startVal = existingOpusValue.afdelingsnummer.startDate != null ? existingOpusValue.afdelingsnummer.startDate : LocalDate.now().toString();
			OpusStatusCodeWrapper status = updateValue(employeeId, existingOpusValue.afdelingsnummer.value, "9021", startVal, stopDateStr);
			if (!status.status.equals(OpusStatusCode.OK)) {
				log.warn("Failed to update " + employeeId + " afdelingsnummer to " + existingOpusValue.afdelingsnummer.value + " when updating stopDate. Message: " + status.message);
			}
		}
	}
	
	private void callOpusAndUpdateEmail(ExistingOpusValue existingOpusValue, String email, String employeeId, Date stopDate) {
		String stopDateStr = "9999-12-31";
		if (stopDate != null) {
			 LocalDate tts = Instant.ofEpochMilli(stopDate.getTime())
		    	      .atZone(ZoneId.systemDefault())
		    	      .toLocalDate();
			 
			 stopDateStr = tts.toString();
		}

		// KMD does not like it when we clear the users original email address, so we will block setting it to the default
		// email address from an actual email address.
		if (Objects.equals(configuration.getModules().getAccountCreation().getOpusHandler().getDefaultEmail(), email)) {
			log.warn("Will not update email on " + employeeId + " because given email was the default email address");
			return;
		}
		
		if (!overwriteEmail(existingOpusValue.email.value)) {
			log.info("Skipping updating " + employeeId + " email to " + email + " because of existing email: " + existingOpusValue.email.value);
			return;
		}
		
		log.info("Updating " + employeeId + " email to " + email);
		OpusStatusCodeWrapper status = updateValue(employeeId, email, "0010", LocalDate.now().toString(), stopDateStr);
		if (!status.status.equals(OpusStatusCode.OK)) {
			log.warn("Failed to update " + employeeId + " email to " + email + ". Message: " + status.message);
		}
	}

	private void callOpusAndUpdatePhones(ExistingOpusValue existingOpusValue, String phone, String departmentNumber, String employeeId, Date stopDate) {
		String stopDateStr = "9999-12-31";
		if (stopDate != null) {
			 LocalDate tts = Instant.ofEpochMilli(stopDate.getTime())
		    	      .atZone(ZoneId.systemDefault())
		    	      .toLocalDate();
			 
			 stopDateStr = tts.toString();
		}

		boolean changes = false;

		if (StringUtils.hasLength(phone)) {
			changes = true;
			OpusStatusCodeWrapper status = updateValue(employeeId, phone, "0020", LocalDate.now().toString(), stopDateStr);
			if (!status.status.equals(OpusStatusCode.OK)) {
				log.warn("Failed to update " + employeeId + " phone to " + phone  + ". Message: " + status.message);
			}
		}
		else if (existingOpusValue.direkteTelefonnummer != null && StringUtils.hasLength(existingOpusValue.direkteTelefonnummer.value)) {
			changes = true;
			OpusStatusCodeWrapper status = updateValue(employeeId, existingOpusValue.direkteTelefonnummer.value, "0020", existingOpusValue.direkteTelefonnummer.startDate, LocalDate.now().toString());
			if (!status.status.equals(OpusStatusCode.OK)) {
				log.warn("Failed to update " + employeeId + " direkteTelefonnummer to stop today. Message: " + status.message);
			}
		}

		if (StringUtils.hasLength(departmentNumber)) {
			changes = true;
			OpusStatusCodeWrapper status = updateValue(employeeId, departmentNumber, "9021", LocalDate.now().toString(), stopDateStr);
			if (!status.status.equals(OpusStatusCode.OK)) {
				log.warn("Failed to update " + employeeId + " departmentNumber to " + departmentNumber + ". Message: " + status.message);
			}
		}
		else if (existingOpusValue.afdelingsnummer != null && StringUtils.hasLength(existingOpusValue.afdelingsnummer.value)) {
			changes = true;
			OpusStatusCodeWrapper status = updateValue(employeeId, existingOpusValue.afdelingsnummer.value, "0020", existingOpusValue.afdelingsnummer.startDate, LocalDate.now().toString());
			if (!status.status.equals(OpusStatusCode.OK)) {
				log.warn("Failed to update " + employeeId + " afdelingsnummer to stop today. Message: " + status.message);
			}
		}

		if (!changes) {
			log.info("No changes on phones detected for " + employeeId);
			return;
		}
		
		log.info("Updating " + employeeId + " phone to " + phone + " and departmentNumber to " + departmentNumber);
	}
	
	private OpusStatusCodeWrapper callOpusAndOrderUser(ExistingOpusValue existingOpusValue, String employeeId, String userId, String email, Date startDate, Date stopDate, String phone, String departmentNumber) {
		String stopDateStr = "9999-12-31";
		if (stopDate != null) {
			 LocalDate tts = Instant.ofEpochMilli(stopDate.getTime())
		    	      .atZone(ZoneId.systemDefault())
		    	      .toLocalDate();
			 
			 stopDateStr = tts.toString();
		}

		LocalDate tts = LocalDate.now();
		if (startDate != null) {
		    LocalDate startDateTts = Instant.ofEpochMilli(startDate.getTime())
		    	      .atZone(ZoneId.systemDefault())
		    	      .toLocalDate();
			
		    // this fixes late-creation (within same Month), where we back-date the start-time to the employement start
		    if (startDateTts.getMonthValue() == tts.getMonthValue() && startDateTts.isBefore(tts)) {
		    	tts = startDateTts;
		    }
		}

		OpusStatusCodeWrapper statusValue = updateValue(employeeId, userId, "0001", tts.toString(), stopDateStr);
		if (!statusValue.status.equals(OpusStatusCode.OK)) {
			return statusValue;
		}

		statusValue = updateValue(employeeId, "IT Bruger", "9905", tts.toString(), stopDateStr);
		if (!statusValue.status.equals(OpusStatusCode.OK)) {
			return statusValue;
		}
		
		if (StringUtils.hasLength(phone)) {
			statusValue = updateValue(employeeId, phone, "0020", tts.toString(), stopDateStr);
			if (!statusValue.status.equals(OpusStatusCode.OK)) {
				log.warn("Failed to set phone when creating OPUS account for employeeId = " + employeeId);
			}
		}

		if (StringUtils.hasLength(departmentNumber)) {
			statusValue = updateValue(employeeId, departmentNumber, "9021", tts.toString(), stopDateStr);
			if (!statusValue.status.equals(OpusStatusCode.OK)) {
				log.warn("Failed to set departmentNumber when creating OPUS account for employeeId = " + employeeId);
			}
		}
		
		if (!overwriteEmail(existingOpusValue.email.value)) {
			String emailValue = existingOpusValue.email.value != null ? existingOpusValue.email.value : email;
			statusValue = updateValue(employeeId, emailValue, "0010", tts.toString(), stopDateStr);
			if (!statusValue.status.equals(OpusStatusCode.OK)) {
				log.warn("Failed to set email when creating OPUS account for employeeId = " + employeeId);
			}
		}
		else {
			statusValue = updateValue(employeeId, email, "0010", tts.toString(), stopDateStr);
			if (!statusValue.status.equals(OpusStatusCode.OK)) {
				log.warn("Failed to set email when creating OPUS account for employeeId = " + employeeId);
			}
		}

		return createWrapper(OpusStatusCode.OK, null);
	}

	private OpusStatusCodeWrapper callOpusRet(String payload) {
    	HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "text/xml; charset=utf-8");
        headers.add("SOAPAction", "http://sap.com/xi/WebService/soap1.1");

    	HttpEntity<String> request = new HttpEntity<String>(payload, headers);
		ResponseEntity<String> response;
		
    	// KMD has some issues, so we might have to try multiple times
    	int tries = 3;
    	do {
    		response = restTemplate.postForEntity(configuration.getModules().getAccountCreation().getOpusHandler().getConvertedUrl(), request, String.class);
			if (response.getStatusCodeValue() != 200) {
				if (--tries >= 0) {
					log.warn("Ret Request - Got responseCode " + response.getStatusCodeValue() + " from service: " + response.getBody());
					
					try {
						Thread.sleep(5000);
					}
					catch (InterruptedException ex) {
						;
					}
				}
				else {
					log.error("Ret Request - Got responseCode " + response.getStatusCodeValue() + " from service: " + response.getBody());
					
					return createWrapper(OpusStatusCode.NETWORK_ERROR, null);
				}
			}
			else {
				break;
			}
    	} while (true);

		String responseBody = response.getBody();		

		try {
			XmlMapper xmlMapper = XmlMapper.builder().configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true).build();

			Envelope envelope = xmlMapper.readValue(responseBody, Envelope.class);
			
			if (envelope.getBody() != null) {
				if (envelope.getBody().getRetOutput() != null) {
					if (envelope.getBody().getRetOutput().getStandardRetur() != null) {
						if (envelope.getBody().getRetOutput().getStandardRetur().getStatusKode() != 0) {
							
							// if we can get better error messages, we can do something else in the future, for now just log as warnings
							if (responseBody.contains("Opdatering af kommunikationsinfotype fejlet")) {
								log.warn("Got error message from OPUS ret operation: " + envelope.getBody().getRetOutput().getStandardRetur().getStatusKode() + "\n" + responseBody + " / request = " + payload);
							}
							else {
								log.error("Got error message from OPUS ret operation: " + envelope.getBody().getRetOutput().getStandardRetur().getStatusKode() + "\n" + responseBody + " / request = " + payload);
							}

							return createWrapper(OpusStatusCode.FAILURE, envelope.getBody().getRetOutput().getStandardRetur().getFejlbeskedTekst());
						}
					}
					else {
						log.error("Got response without status on OPUS ret operation:\n" + responseBody);

						return createWrapper(OpusStatusCode.FAILURE, "Ingen status i svar fra OPUS");
					}
				}
			}
			else {
				log.error("Got response without body on OPUS ret operation:\n" + responseBody);
				
				return createWrapper(OpusStatusCode.FAILURE, "Intet svar fra OPUS");
			}
		}
		catch (Exception ex) {
			log.error("Failed to parse OPUS rest operation response\n" + responseBody, ex);
			
			return createWrapper(OpusStatusCode.FAILURE, "Uventet svar fra OPUS");
		}

		return createWrapper(OpusStatusCode.OK, null);
	}
	
	private boolean overwriteEmail(String existingEmail) {
		if (!StringUtils.hasLength(existingEmail) || configuration.getModules().getAccountCreation().getOpusHandler().isOverwriteExistingEmails()) {
			return true;
		}

		if (StringUtils.hasLength(configuration.getModules().getAccountCreation().getOpusHandler().getDefaultEmail())) {
			// if the existing email ends with the configured default email, overwriting is allowed
			return existingEmail.endsWith(configuration.getModules().getAccountCreation().getOpusHandler().getDefaultEmail());
		}

		// no, overwriting existing emails is not allowed
		return false;
	}

	// returns "" on no phone
	private String getPhone(Person person) {
		String phone = person.getPhones().stream().filter(p -> p.getPhone().getPhoneType().equals(PhoneType.LANDLINE) && p.getPhone().isTypePrime()).map(p -> p.getPhone().getPhoneNumber()).findAny().orElse(null);
		if (phone == null) {
			phone = person.getPhones().stream().filter(p -> p.getPhone().getPhoneType().equals(PhoneType.MOBILE) && p.getPhone().isTypePrime()).map(p -> p.getPhone().getPhoneNumber()).findAny().orElse("");
		}

		return phone;
	}

	// returns "" on no phone
	private String getDepartmentPhone(Person person) {
		return person.getPhones().stream().filter(p -> p.getPhone().getPhoneType().equals(PhoneType.DEPARTMENT_NUMBER) && p.getPhone().isTypePrime()).map(p -> p.getPhone().getPhoneNumber()).findAny().orElse("");
	}

	private OpusStatusCodeWrapper createWrapper(OpusStatusCode status, String message) {
		OpusStatusCodeWrapper wrapper = new OpusStatusCodeWrapper();
		wrapper.status = status;
		wrapper.message = message;

		return wrapper;
	}
	
	public LocalDateTime convertToLocalDateTime(Date dateToConvert) {
	    return dateToConvert.toInstant()
	      .atZone(ZoneId.systemDefault())
	      .toLocalDateTime();
	}
	
	public Date convertToDate(LocalDateTime dateToConvert) {
	    return java.util.Date
	      .from(dateToConvert.atZone(ZoneId.systemDefault())
	      .toInstant());
	}
	
	private OpusStatusCodeWrapper updateValue(String employeeId, String value, String code, String startDateStr, String stopDateStr) {
		StringBuilder builder = new StringBuilder();

		builder.append(OPUS_SOAP_BEGIN);
		builder.append(OPUS_SOAP_RET_BEGIN);
		
		builder.append(OPUS_SOAP_MODTAGER_REF.replace("{0}", configuration.getModules().getAccountCreation().getOpusHandler().getMunicipalityNumber()));
		builder.append(OPUS_SOAP_MEDARBEJDER_REF.replace("{0}", employeeId));
		
		builder.append(OPUS_SOAP_ATTRIBUTLISTE_BEGIN);

		builder.append(OPUS_SOAP_KOMMUNIKATION
				.replace("{0}", startDateStr)
				.replace("{1}", stopDateStr)
				.replace("{2}", code)
				.replace("{3}", value));
		
		builder.append(OPUS_SOAP_ATTRIBUTLISTE_END);

		builder.append(OPUS_SOAP_RET_END);
		builder.append(OPUS_SOAP_END);
		String payload = builder.toString();

		return callOpusRet(payload);
	}
}
