package dk.digitalidentity.sofd.controller.rest.model;

import java.util.Date;

import dk.digitalidentity.sofd.dao.model.enums.LeaveReason;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LeaveDTO {
	private boolean leave;
	private Date leaveStartDate;
	private Date leaveStopDate;
	private LeaveReason reason;
	private String reasonText;
	private boolean disableAccountOrders;
	private boolean expireAccounts;
}
