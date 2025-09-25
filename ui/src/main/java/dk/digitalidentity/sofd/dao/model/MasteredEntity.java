package dk.digitalidentity.sofd.dao.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import dk.digitalidentity.sofd.dao.model.mapping.MappableEntity;
import dk.digitalidentity.sofd.dao.model.mapping.MappedEntity;

import javax.validation.constraints.NotNull;

public abstract class MasteredEntity extends MappedEntity implements MappableEntity {
	public abstract String getMaster();
	public abstract String getMasterId();
	public abstract void setMaster(@NotNull String master);
	public abstract void setMasterId(@NotNull String masterId);
	
	// hack to make our generic method in PersonApi and OrgUnitApi happy
	@JsonIgnore
	@Override
	public MasteredEntity getEntity() {
		return this;
	}
}
