package dk.digitalidentity.sofd.controller.api.v2.model;

import javax.validation.constraints.NotNull;

import dk.digitalidentity.sofd.dao.model.Email;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class EmailApiRecord extends BaseRecord {
	
	// primary key
	
	@NotNull
	private String master;

	@NotNull
	private String masterId;

	// read/write fields
	
	@NotNull
	private String email;

	// read-only
	private boolean prime;

	public EmailApiRecord(Email email) {
		this.master = email.getMaster();
		this.masterId = email.getMasterId();
		this.email = email.getEmail();
		this.prime = email.isPrime();
	}

	public Email toEmail() {
		Email email = new Email();
		email.setEmail(this.email);		
		email.setMaster(master);
		email.setMasterId(masterId);
		
		return email;
	}
}
