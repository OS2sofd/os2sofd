package dk.digitalidentity.sofd.controller.mvc.dto;

import java.util.Date;
import java.util.List;

import dk.digitalidentity.sofd.controller.rest.model.PhoneDTO;
import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.enums.LeaveReason;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonDTO {
	private String uuid;
	private String cpr;
	private String master;
	private String firstname;
	private String surname;
	private String chosenName;
	private String keyWords;
	private String notes;
	private boolean taxedPhone;
	private Date firstEmploymentDate;
	private Date anniversaryDate;
	private String workAddress;
	private PostDTO registeredPostAddress;
	private PostDTO residencePostAddress;
	private Date created;
	private Date lastChanged;
	
	private boolean leave;
	private Date leaveStartDate;
	private Date leaveStopDate;
	private LeaveReason leaveReason;
	private String leaveReasonText;
	private boolean leaveExpireAccounts;
	private boolean leaveDisableAccountOrders;
	private boolean personUuidNotKombitUuid;

	private boolean forceStop;
	private String stopReason;
	private boolean disableAccountOrdersCreate;
	private boolean disableAccountOrdersDisable;
	private boolean disableAccountOrdersDelete;
	private List<Affiliation> affiliations;
	private List<UserDTO> users;
	private List<PhoneDTO> phones;
	private List<SubstituteAssignmentDTO> substituteAssignments;

	private List<LocalExtensionDTO> localExtensions;
	
	private List<AuthorizationCodeDTO> authorizationCodes;
}
