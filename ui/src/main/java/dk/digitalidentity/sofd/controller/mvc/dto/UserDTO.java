package dk.digitalidentity.sofd.controller.mvc.dto;

import java.time.LocalDate;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserDTO {
	private long id;
	private String uuid;
	private String userId;
	private String userType;
	private String employeeId;
	private String employeeIdDisplay;
	private boolean prime;
	private Boolean disabled;
	private Boolean passwordLocked;
	private boolean pendingDeactivation;
	private boolean substituteAccount;
	private LocalDate accountExpireDate;
	private boolean pending;
	private UserChangeEmployeeIdQueueDTO userChangeEmployeeIdQueueDTO;
	private String kombitUuid;

	public boolean isDisabled() {
		return (disabled != null && disabled == true);
	}

	public boolean isExpired() {
		return (accountExpireDate != null && (accountExpireDate.isEqual(LocalDate.now()) || accountExpireDate.isBefore(LocalDate.now())));
	}
}
