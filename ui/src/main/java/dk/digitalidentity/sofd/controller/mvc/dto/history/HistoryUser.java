package dk.digitalidentity.sofd.controller.mvc.dto.history;

import dk.digitalidentity.sofd.dao.model.User;
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

	public HistoryUser(User user) {
		this.uuid = user.getUuid();
		this.master = user.getMaster();
		this.masterId = user.getMasterId();
		this.userId = user.getUserId();
		this.employeeId = user.getEmployeeId();
		this.userType = user.getUserType();
		this.prime = user.isPrime();
		this.disabled = user.isDisabled();
	}
}
