package dk.digitalidentity.sofd.controller.api.v2.model;

import dk.digitalidentity.sofd.dao.model.OrgUnitManager;
import dk.digitalidentity.sofd.dao.model.Person;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ManagerApiRecord {
	private String uuid;
	private String name;
	private boolean inherited;

    public OrgUnitManager toOrgUnitManager() {
        OrgUnitManager orgUnitManager = new OrgUnitManager();
        Person manager = new Person();
        manager.setUuid(uuid);
        orgUnitManager.setManager(manager);
        orgUnitManager.setName(name);
        orgUnitManager.setInherited(inherited);
        return orgUnitManager;
    }
}
