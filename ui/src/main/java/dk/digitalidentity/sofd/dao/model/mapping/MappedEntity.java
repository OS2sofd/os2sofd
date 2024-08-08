package dk.digitalidentity.sofd.dao.model.mapping;

import dk.digitalidentity.sofd.dao.model.MasteredEntity;

public abstract class MappedEntity implements MappableEntity {
	public abstract MasteredEntity getEntity();
}
