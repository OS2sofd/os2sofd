package dk.digitalidentity.sofd.service.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OUTagRow {
	private String id;
	private String parent;
	private String text;
	private String manager;
	private Long tagId;
	private String tagValue;
}
