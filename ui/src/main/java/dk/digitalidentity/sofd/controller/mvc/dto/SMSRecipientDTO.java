package dk.digitalidentity.sofd.controller.mvc.dto;

import java.util.HashSet;
import java.util.Set;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SMSRecipientDTO {
	private String uuid;
	private String name;
	private Set<String> phones;
	private String email;
	private RecipientType type;
	private boolean selected = true;

	private Set<RecipientCriteria> criteria = new HashSet<>();

	public enum RecipientCriteria {
		ORGUNIT,FUNCTION,IT_SYSTEM
	}
}
