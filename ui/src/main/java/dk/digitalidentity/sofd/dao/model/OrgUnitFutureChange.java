package dk.digitalidentity.sofd.dao.model;

import dk.digitalidentity.sofd.dao.model.enums.AppliedStatus;
import dk.digitalidentity.sofd.dao.model.enums.OrgUnitAttribute;
import dk.digitalidentity.sofd.dao.model.enums.OrgUnitChangeType;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Getter
@Setter
@Entity(name = "orgunit_change_queue")
public class OrgUnitFutureChange {

    @Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column
    @NotNull
    private String orgunitUuid;

    @Column
    @NotNull
    private String orgunitName;
    
    @Column
    private String displayName;

    @Column
    @NotNull
    private Date changeDate;

    @Column
    @Enumerated(EnumType.STRING)
    private OrgUnitChangeType changeType;

    @Column
    @Enumerated(EnumType.STRING)
    private OrgUnitAttribute attributeField;

    @Column
    private String attributeValue;

    @Column
    private String createPayload;

    @Column
    private String parentUuid;

    @Column
    private String parentName;

    @Column
    @Enumerated(EnumType.STRING)
    private AppliedStatus appliedStatus = AppliedStatus.NOT_APPLIED;

    @Column
    private Date appliedDate;

    @Column
    private Long tagId;

    @Column
    private String tagValue;

    public OrgUnitFutureChange() {

    }

    public OrgUnitFutureChange(String orgunitUuid, String orgunitName, String parentUuid, String parentName, Date changeDate) {
        this.changeDate = changeDate;
        this.changeType = OrgUnitChangeType.MOVE;
        this.appliedStatus = AppliedStatus.NOT_APPLIED;
        this.orgunitUuid = orgunitUuid;
        this.orgunitName = orgunitName;

        this.parentUuid = parentUuid;
        this.parentName = parentName;
    }

    public OrgUnitFutureChange(String orgunitUuid, String orgunitName, String createPayload, Date changeDate, String parentUuid, String parentName, String displayName) {
        this.changeDate = changeDate;
        this.changeType = OrgUnitChangeType.CREATE;
        this.appliedStatus = AppliedStatus.NOT_APPLIED;
        this.orgunitUuid = orgunitUuid;
        this.orgunitName = orgunitName;
        this.parentUuid = parentUuid;
        this.parentName = parentName;

        this.createPayload = createPayload;
        this.displayName = displayName;
    }

    public OrgUnitFutureChange(String orgunitUuid, String orgunitName, OrgUnitAttribute attributeField, String attributeValue, Date changeDate) {
        this.changeDate = changeDate;
        this.changeType = OrgUnitChangeType.UPDATE_ATTRIBUTE;
        this.appliedStatus = AppliedStatus.NOT_APPLIED;
        this.orgunitUuid = orgunitUuid;
        this.orgunitName = orgunitName;

        this.attributeField = attributeField;
        this.attributeValue = attributeValue;
    }
}
