package dk.digitalidentity.sofd.log;

import dk.digitalidentity.sofd.dao.model.enums.EntityType;

public interface Loggable {
	String getEntityId();
	String getEntityName();
	EntityType getEntityType();
	String getEntityLogInfo();
}