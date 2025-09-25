package dk.digitalidentity.sofd.service.model;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OUTreeFormWithTags {
	private String id;
	private String parent;
	private String text;
	private String manager;
	private List<Long> tagIds;
	private Map<Long, String> tagValueMap;
}
