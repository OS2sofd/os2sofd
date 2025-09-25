package dk.digitalidentity.sofd.service.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SystemRoleDTO {
	private long id;
	private String name;
	private String identifier;
	private String description;
	private long weight;
}
