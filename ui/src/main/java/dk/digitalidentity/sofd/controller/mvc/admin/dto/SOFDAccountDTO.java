package dk.digitalidentity.sofd.controller.mvc.admin.dto;

import javax.validation.constraints.Size;

import dk.digitalidentity.sofd.dao.model.SOFDAccount;
import dk.digitalidentity.sofd.service.PersonService;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SOFDAccountDTO {
	private long id;
	private String personName;
	
	@Size(min = 36, max = 36, message = "Der skal vælges en tilknyttet person")
	private String personUUID;
	
	@Size(min = 2, max = 36, message = "Der skal vælges et bruger-id på mindst 2 tegn (og max 36 tegn)")
	private String userId;
	
	@Size(min = 8, max = 36, message = "Der skal vælges et kodeord på mindst 8 tegn (og max 36 tegn)")
	private String password;

	public SOFDAccountDTO(SOFDAccount account) {
		this.id = account.getId();
		this.personName = PersonService.getName(account.getPerson());
		this.personUUID = account.getPerson().getUuid();
		this.userId = account.getUserId();
	}
}
