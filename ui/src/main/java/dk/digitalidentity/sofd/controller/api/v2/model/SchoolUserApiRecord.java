package dk.digitalidentity.sofd.controller.api.v2.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.digitalidentity.sofd.dao.model.SchoolUser;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class SchoolUserApiRecord extends BaseRecord {

	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private static final ObjectMapper OBJECT_MAPPER;

	static {
		OBJECT_MAPPER = new ObjectMapper();
	}

	private String uuid;
	private String username;
	private boolean disabled;
	private String name;
	private String title;
	private String cpr;
	private List<String> institutionNumbers;

	public SchoolUserApiRecord(SchoolUser schoolUser) {
		this.uuid = schoolUser.getUuid();
		this.username = schoolUser.getUserId();
		this.disabled = schoolUser.isDisabled();
		this.name = schoolUser.getName();
		this.title = schoolUser.getTitle();
		this.cpr = schoolUser.getCpr();
		this.institutionNumbers = getInstitutionNumbers(schoolUser.getLocalExtensions());
	}

	private List<String> getInstitutionNumbers(String localExtensions) {
		var result = new ArrayList<String>();
		try {
			Map<String, String> map = OBJECT_MAPPER.readValue(localExtensions, Map.class);
			// Institutionsnumre is a local extension that can be set by the STIL integration
			var institutionNumbers = map.get("Institutionsnumre");
			if (institutionNumbers != null) {
				result.addAll(List.of(institutionNumbers.split(",")));
			}
		}
		catch (Exception e) {
			// ignore
		}
		return result;
	}
}
