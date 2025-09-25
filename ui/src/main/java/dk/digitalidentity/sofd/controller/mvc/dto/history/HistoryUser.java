package dk.digitalidentity.sofd.controller.mvc.dto.history;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import dk.digitalidentity.sofd.dao.model.User;
import dk.digitalidentity.sofd.serializer.LocalExtensionsDeserializer;
import dk.digitalidentity.sofd.serializer.LocalExtensionsSerializer;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HistoryUser {
	private String uuid;
	private String master;
	private String masterId;
	private String userId;
	private String employeeId;
	private String userType;
	private boolean prime;
	private boolean disabled;
	@JsonSerialize(using = LocalExtensionsSerializer.class)
	@JsonDeserialize(using = LocalExtensionsDeserializer.class)
	private String localExtensions;


	public HistoryUser(User user) {
		this.uuid = user.getUuid();
		this.master = user.getMaster();
		this.masterId = user.getMasterId();
		this.userId = user.getUserId();
		this.employeeId = user.getEmployeeId();
		this.userType = user.getUserType();
		this.prime = user.isPrime();
		this.disabled = user.isDisabled();
		this.localExtensions = user.getLocalExtensions();
	}
}
