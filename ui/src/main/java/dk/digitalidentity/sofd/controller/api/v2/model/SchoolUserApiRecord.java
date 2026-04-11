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
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private String uuid;
	private String username;
	private boolean disabled;
	private String name;
	private String title;
	private String cpr;
	private List<String> institutionNumbers;

	// if a municipality wants a different priority at some point, then move this setting to municipality entity in db
	private static final List<String> ROLE_PRIORITY = List.of(
			"Lærer", "TAP", "Ledelse", "Leder", "Pædagog", "Vikar", "Konsulent"
	);


	public SchoolUserApiRecord(SchoolUser schoolUser) {
		this.uuid = schoolUser.getUuid();
		this.username = schoolUser.getUserId();
		this.disabled = schoolUser.isDisabled();
		this.name = schoolUser.getName();
		this.title = getTitle(schoolUser);
		this.cpr = schoolUser.getCpr();
		this.institutionNumbers = getInstitutionNumbers(schoolUser.getLocalExtensions());
	}

	@SuppressWarnings("unchecked")
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

	private String getTitle(SchoolUser schoolUser) {
		var result = schoolUser.getTitle();
		try {
			Map<String, String> map = OBJECT_MAPPER.readValue(schoolUser.getLocalExtensions(), Map.class);
			// Roller is a local extension that can be set by the STIL integration
			var institutionNumbers = map.get("Roller");
			if (institutionNumbers != null) {
				var allRoles = List.of(institutionNumbers.split(","));

				var primaryRole = ROLE_PRIORITY.stream()
						.filter(allRoles::contains)
						.findFirst()
						.orElse(null);

				if (primaryRole != null) {
					result = primaryRole;
				}
			}
		}
		catch (Exception e) {
			// ignore
		}
		return result;
	}

}
