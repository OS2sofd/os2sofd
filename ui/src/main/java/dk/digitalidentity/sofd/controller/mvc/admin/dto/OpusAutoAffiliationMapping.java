package dk.digitalidentity.sofd.controller.mvc.admin.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OpusAutoAffiliationMapping {
	private String fromUuid;
	private String fromName;
	private String toUuid;
	private String toName;
}
