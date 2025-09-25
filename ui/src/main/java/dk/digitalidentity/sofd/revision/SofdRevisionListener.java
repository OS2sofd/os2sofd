package dk.digitalidentity.sofd.revision;

import dk.digitalidentity.sofd.security.SecurityUtil;
import org.hibernate.envers.RevisionListener;

public class SofdRevisionListener implements RevisionListener {

	public void newRevision(Object revisionEntity) {
		SofdRevision entity = (SofdRevision) revisionEntity;
		String auditorId;
		String auditorName;

		if (SecurityUtil.getClient() != null) {
			auditorId = SecurityUtil.getClient().getEntityId();
			auditorName = SecurityUtil.getClient().getName();
		}
		else if (SecurityUtil.getUser() != null) {
			auditorId = SecurityUtil.getUser();
			auditorName = SecurityUtil.getUser();
		}
		else {
			auditorId = "SYSTEM";
			auditorName = "Scheduled task";
		}
		
		if (auditorId.length() > 128) {
			auditorId = auditorId.substring(0, 128);
		}
		
		if (auditorName.length() > 128) {
			auditorName = auditorName.substring(0, 128);
		}

		entity.setAuditorId(auditorId);
		entity.setAuditorName(auditorName);
	}
}