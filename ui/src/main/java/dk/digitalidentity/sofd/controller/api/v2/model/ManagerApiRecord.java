package dk.digitalidentity.sofd.controller.api.v2.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ManagerApiRecord {
	private String uuid;
	private String name;
	private boolean inherited;
}
