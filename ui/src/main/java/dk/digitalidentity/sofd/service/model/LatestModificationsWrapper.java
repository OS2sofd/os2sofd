package dk.digitalidentity.sofd.service.model;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LatestModificationsWrapper {
	private Long latestRevisionNumber;
	private List<AuditWrapper> personChanges;
	private List<AuditWrapper> orgUnitChanges;
}
