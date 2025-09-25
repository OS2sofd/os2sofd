package dk.digitalidentity.sofd.dao.model.enums;

public enum OrgUnitAttribute {
    NAME("html.entity.orgunit.name"),
    SHORT_NAME("html.entity.orgunit.shortname"),
    TYPE("html.entity.orgunit.orgtype"),
    CVR("html.entity.orgunit.cvr"),
    SENR("html.entity.orgunit.senr"),
    PNR("html.entity.orgunit.pnr"),
    COST_BEARER("html.entity.orgunit.costbearer"),
    PARENT("html.entity.orgunit.parent"),
    BELONGS_TO("html.entity.orgunit.belongsto"),
    DISPLAY_NAME("html.entity.orgunit.displayName"),
    MANAGER("html.entity.orgunit.manager");

    private String message;

    private OrgUnitAttribute(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}