package dk.digitalidentity.sofd.controller.mvc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePersonDTO {
	private String uuid;
	private String cpr;
	private String firstname;
	private String surname;
	private PostDTO registeredPostAddress;
	private AffiliationDTO affiliation;
}
