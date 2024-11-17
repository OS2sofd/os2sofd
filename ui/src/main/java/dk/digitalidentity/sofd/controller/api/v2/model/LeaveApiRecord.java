package dk.digitalidentity.sofd.controller.api.v2.model;

import dk.digitalidentity.sofd.dao.model.PersonLeave;
import dk.digitalidentity.sofd.dao.model.enums.LeaveReason;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;

@Getter
@Setter
@ToString
public class LeaveApiRecord extends BaseRecord{
    private long id;
    private Date startDate;
    private Date stopDate;
    private LeaveReason reason;
    private String reasonText;
    private boolean disableAccountOrders;
    private boolean disableAccountOrdersDisable;
    private boolean disableAccountOrdersDelete;
    private boolean expireAccounts;

    public LeaveApiRecord(PersonLeave leave) {
        this.id = leave.getId();
        this.startDate = leave.getStartDate();
        this.stopDate = leave.getStopDate();
        this.reason = leave.getReason();
        this.reasonText = leave.getReasonText();
        this.disableAccountOrders = leave.isDisableAccountOrders();
        this.expireAccounts = leave.isExpireAccounts();
    }
}