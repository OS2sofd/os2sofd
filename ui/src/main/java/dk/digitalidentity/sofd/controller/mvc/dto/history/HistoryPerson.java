package dk.digitalidentity.sofd.controller.mvc.dto.history;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.Phone;
import dk.digitalidentity.sofd.dao.model.User;
import dk.digitalidentity.sofd.service.PersonService;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HistoryPerson {
	private String uuid;
	private String master;
	private Date created;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy HH:mm:ss")
	private Date lastChanged;
	private boolean deleted;
	private String cpr;
	private String firstname;
	private String surname;
	private String chosenName;
	private boolean forceStop;
	private boolean disableAccountOrders;
	private HistoryPost registeredPostAddress;
	private HistoryPost residencePostAddress;
	private List<HistoryPhone> phones;
	private List<HistoryUser> users;
	private List<HistoryAffiliation> affiliations;

	public HistoryPerson(Person person) {
		this.uuid = person.getUuid();
		this.master = person.getMaster();
		this.created = person.getCreated();
		this.lastChanged = person.getLastChanged();
		this.deleted = person.isDeleted();
		this.cpr = PersonService.maskCpr(person.getCpr());
		this.firstname = person.getFirstname();
		this.surname = person.getSurname();
		this.chosenName = person.getChosenName();
		this.forceStop = person.isForceStop();
		this.disableAccountOrders = person.isDisableAccountOrdersCreate();
		this.registeredPostAddress = (person.getRegisteredPostAddress() != null) ? new HistoryPost(person.getRegisteredPostAddress()) : null;
		this.residencePostAddress = (person.getResidencePostAddress() != null) ? new HistoryPost(person.getResidencePostAddress()) : null;
		this.phones = new ArrayList<HistoryPhone>();
		this.users = new ArrayList<HistoryUser>();
		this.affiliations = new ArrayList<HistoryAffiliation>();
		
		for (Phone phone : PersonService.getPhones(person)) {
			this.phones.add(new HistoryPhone(phone));
		}
		
		for (User user : PersonService.getUsers(person)) {
			this.users.add(new HistoryUser(user));
		}
		
		if (person.getAffiliations() != null) {
			for (Affiliation affiliation : person.getAffiliations()) {
				this.affiliations.add(new HistoryAffiliation(affiliation));
			}
		}
	}	
}
