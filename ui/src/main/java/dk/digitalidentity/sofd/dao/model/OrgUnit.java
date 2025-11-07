package dk.digitalidentity.sofd.dao.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import dk.digitalidentity.sofd.dao.model.enums.EntityType;
import dk.digitalidentity.sofd.dao.model.mapping.OrgUnitPhoneMapping;
import dk.digitalidentity.sofd.dao.model.mapping.OrgUnitPostMapping;
import dk.digitalidentity.sofd.dao.model.mapping.OrgUnitPrimaryKleMapping;
import dk.digitalidentity.sofd.dao.model.mapping.OrgUnitSecondaryKleMapping;
import dk.digitalidentity.sofd.dao.model.mapping.OrgUnitTertiaryKleMapping;
import dk.digitalidentity.sofd.log.Loggable;
import dk.digitalidentity.sofd.serializer.LocalExtensionsDeserializer;
import dk.digitalidentity.sofd.serializer.LocalExtensionsSerializer;
import lombok.Getter;
import lombok.Setter;

@Entity(name = "orgunits")
@Getter
@Setter
@Audited
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" }) // need this because we sometimes detach the object from Hibernate
public class OrgUnit implements Loggable {

	@Id
	private String uuid;

	@GeneratedValue
	private long id;

	@Column
	@NotNull
	private String master;

	@Column
	@NotNull
	private String masterId;

	@Column
	private boolean deleted;

	@CreationTimestamp
	@Temporal(TemporalType.TIMESTAMP)
	@Column(updatable = false)
	private Date created;

	@Temporal(TemporalType.TIMESTAMP)
	@Column
	private Date lastChanged;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_uuid")
	private OrgUnit parent;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "belongs_to")
	private Organisation belongsTo;

	@OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
	@Valid
	private List<OrgUnit> children;

	@Column
	@Size(max = 64)
	@NotNull
	private String shortname;

	@Column
	@NotNull
	@Size(max = 255)
	private String name;

	@Column
	@Size(max = 255)
	private String displayName;

	@Column
	@NotNull
	@Size(max = 255)
	private String sourceName;

	@Column
	private Long cvr;

	@Column
	@Size(max = 255)
	private String cvrName;

	@Column
	private Long senr;

	@Column
	private Long pnr;

	@Column
	@Size(max = 255)
	private String costBearer;

	@Column
	@Size(max = 255)
	private String orgType;

	@Column
	private Long orgTypeId;

	@Column
	private String keyWords;

	@Column
	private String notes;

	@Column
	private String openingHours;

	@Column
	private String contactAddress;

	@Column
	private boolean inheritKle;

	@Column
	private String location;

	@Column
	private String urlAddress;

	@Column
	private String openingHoursPhone;

	@Column
	private String emailNotes;

	@Column
	private String email;

	@Column
	private boolean doNotTransferToFkOrg;

	@Column
	private boolean blockUpdate;

	@BatchSize(size = 100)
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "orgUnit")
	@Valid
	private List<OrgUnitPostMapping> postAddresses;

	@BatchSize(size = 100)
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "orgUnit")
	@Valid
	private List<OrgUnitPhoneMapping> phones;

	@BatchSize(size = 100)
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "orgUnit", orphanRemoval = true)
	@JsonIgnore
	private List<OrgUnitTag> tags;

	@BatchSize(size = 50)
	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "orgUnit", orphanRemoval = true)
	@Valid
	@NotAudited
	private OrgUnitManager manager;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "orgunit_type_id")
	private OrgUnitType type;

	// used for imports from external systems (OPUS, SD)
	@Column
	private String importedManagerUuid;

	// used for manually selected managers (API or GUI)
	@Column
	private String selectedManagerUuid;

	@Transient
	@JsonIgnore
	private boolean inheritedEan;

	@BatchSize(size = 100)
	@OneToMany(mappedBy = "orgUnit", fetch = FetchType.LAZY)
	@Valid
	private List<Affiliation> affiliations;

	@BatchSize(size = 100)
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "orgUnit")
	private List<OrgUnitPrimaryKleMapping> klePrimary;

	@BatchSize(size = 100)
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "orgUnit")
	private List<OrgUnitSecondaryKleMapping> kleSecondary;

	@BatchSize(size = 100)
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "orgUnit")
	private List<OrgUnitTertiaryKleMapping> kleTertiary;

	@Column
	@JsonSerialize(using = LocalExtensionsSerializer.class)
	@JsonDeserialize(using = LocalExtensionsDeserializer.class)
	private String localExtensions;

	@BatchSize(size = 100)
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "orgUnit")
	private List<ManagedTitle> managedTitles;

	@BatchSize(size = 100)
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "orgUnit", orphanRemoval = true)
	@JsonIgnore
	private List<SubstituteOrgUnitAssignment> substitutes = new ArrayList<>();

	@BatchSize(size = 100)
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "orgUnit")
	@Valid
	private List<Ean> eanList;
	
	@Transient
	private List<Ean> inheritedEanList;

	@JsonIgnore
	@Override
	public String getEntityId() {
		return uuid;
	}

	@JsonIgnore
	@Override
	public EntityType getEntityType() {
		return EntityType.ORGUNIT;
	}

	@Override
	public String getEntityLogInfo() {
		var sb = new StringBuilder();
		sb.append("uuid: ").append(uuid).append(", ");
		sb.append("name: ").append(name).append(", ");
		return sb.toString();
	}

	@Override
    public boolean equals(Object o) {
        if (o == null) {
        	return false;
        }

        if (this == o) {
        	return true;
        }

        if (!Objects.equals(getClass(), o.getClass())) {
            return false;
        }

        OrgUnit that = (OrgUnit) o;

        return this.uuid != null && Objects.equals(this.uuid, that.uuid);
    }

    @Override
    public int hashCode() {
    	// this is always non-null
    	if (this.masterId != null) {
    		return masterId.hashCode();
    	}

    	// but safety first
        return getClass().hashCode();
    }

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
		updateName();
	}

	public void setSourceName(String sourceName) {
		this.sourceName = sourceName;
		updateName();
	}

	public void setName(String name) throws Exception {
		throw new Exception("name should not be set explicitly");
	}

	private void updateName() {
		this.name = displayName != null && !displayName.isEmpty() ? displayName : sourceName;
	}

	@Override
	public String getEntityName() {
		return (displayName != null && !displayName.isEmpty()) ? displayName : sourceName;
	}

}
