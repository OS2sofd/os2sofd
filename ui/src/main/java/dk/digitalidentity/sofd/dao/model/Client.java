package dk.digitalidentity.sofd.dao.model;

import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.hibernate.envers.Audited;

import dk.digitalidentity.sofd.dao.model.enums.AccessRole;
import dk.digitalidentity.sofd.dao.model.enums.EntityType;
import dk.digitalidentity.sofd.dao.model.enums.VersionStatus;
import dk.digitalidentity.sofd.log.Loggable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Audited
public class Client implements Loggable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column
	private String name;

	@Column
	private String apiKey;

	@Column
	@Enumerated(EnumType.STRING)
	private AccessRole accessRole;

	@OneToMany(mappedBy = "client", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
	private List<AccessField> accessFieldList;
	
	@Column
	private boolean showOnFrontpage;

	@Column
	private boolean monitorForActivity;
	
	@Column
	private Date lastActive;
	
	@Column
	private String version;
	
	@Column(nullable = true)
	private String tlsVersion;

	@Column
	private String applicationIdentifier;

	@Column
	private String newestVersion;

	@Column
	private String minimumVersion;

	@Column
	@Enumerated(EnumType.STRING)
	private VersionStatus versionStatus;
	private int warningStateHours;

	@Column
	private int errorStateHours;

	@Override
	public String getEntityId() {
		return Long.toString(id);
	}

	@Override
	public EntityType getEntityType() {
		return EntityType.CLIENT;
	}

	@Override
	public String getEntityName() {
		return name;
	}
}
