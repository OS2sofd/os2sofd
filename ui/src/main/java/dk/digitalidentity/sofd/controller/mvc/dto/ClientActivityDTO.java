package dk.digitalidentity.sofd.controller.mvc.dto;

import java.util.Calendar;
import java.util.Date;

import dk.digitalidentity.sofd.dao.model.Client;
import dk.digitalidentity.sofd.dao.model.enums.VersionStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClientActivityDTO {
	private String name;
	private Date lastActive;
	private String color;
	private String colorDark;
	private String version;
	private boolean outdated;
	
	public ClientActivityDTO(Client client) {
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.HOUR_OF_DAY, -client.getWarningStateHours());
		Date warningDate = calendar.getTime();
		calendar.setTime(new Date());
		calendar.add(Calendar.HOUR_OF_DAY, -client.getErrorStateHours());
		Date errorDate = calendar.getTime();
		
		if (client.getLastActive() == null || client.getLastActive().before(errorDate)) {
			color = "bg-red";
			colorDark = "bg-red-dark";
		}
		else if (client.getLastActive().before(warningDate)) {
			color = "bg-yellow";
			colorDark = "bg-yellow-dark";
		}
		else {
			color = "bg-primary";
			colorDark = "bg-primary-dark";
		}
		
		name = client.getName();
		lastActive = client.getLastActive();
		version = client.getVersion();
		outdated = client.getVersionStatus() == VersionStatus.OUTDATED;
	}
}
