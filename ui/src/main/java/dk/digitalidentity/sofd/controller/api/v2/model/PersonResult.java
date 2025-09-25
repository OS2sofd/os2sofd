package dk.digitalidentity.sofd.controller.api.v2.model;

import java.util.Set;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class PersonResult {
	private Set<PersonApiRecord> persons;
	private long page;
	private String nextOffset;
}
