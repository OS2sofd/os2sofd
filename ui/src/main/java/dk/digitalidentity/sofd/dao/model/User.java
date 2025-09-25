package dk.digitalidentity.sofd.dao.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.BatchSize;
import org.hibernate.envers.Audited;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import dk.digitalidentity.sofd.serializer.LocalExtensionsDeserializer;
import dk.digitalidentity.sofd.serializer.LocalExtensionsSerializer;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity(name = "users")
@Getter
@Setter
@Audited
@BatchSize(size = 100)
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class User extends MasteredEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column
	@NotNull
	private String uuid;
	
	@Column
	@NotNull
	private String master;
	
	@Column
	@NotNull
	private String masterId;

	@Column
	@NotNull
	@Size(max = 64)
	private String userId;
	
	@Column
	@Size(max = 255)
	private String employeeId;

	@NotNull
	@Column
	private String userType;

	@Column
	@JsonSerialize(using = LocalExtensionsSerializer.class)
	@JsonDeserialize(using = LocalExtensionsDeserializer.class)
	private String localExtensions;
	
	@BatchSize(size = 100)
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "user")
	private List<ActiveDirectoryDetails> activeDirectoryDetails;

	@Column
	@NotNull
	private boolean prime;

	// TODO: this does not appear to be used for anything (yet?)
	@Column
	@NotNull
	private boolean substituteAccount;

	@Column
	@NotNull
	private boolean disabled;
	
	// TODO: temporary hack to ensure we can deal with null values in Better API
	private transient Boolean tSubstituteAccount;
	private transient Boolean tDisabled;

	public void setEmployeeId(String employeeId) {
		this.employeeId = StringUtils.isBlank(employeeId) ? null : employeeId;
	}

	public boolean isExpired() {
		var expired = false;
		if (getActiveDirectoryDetails() != null && getActiveDirectoryDetails().getAccountExpireDate() != null ) {
			expired = getActiveDirectoryDetails().getAccountExpireDate().isBefore(LocalDate.now().plusDays(1));
		}
		return expired;
	}

	public ActiveDirectoryDetails getActiveDirectoryDetails() {
		if (activeDirectoryDetails != null && activeDirectoryDetails.size() > 0) {
			return activeDirectoryDetails.get(0);
		}
		
		return null;
	}
	
	public void setActiveDirectoryDetails(ActiveDirectoryDetails details) {
		if (activeDirectoryDetails == null) {
			activeDirectoryDetails = new ArrayList<ActiveDirectoryDetails>();
		}
		
		activeDirectoryDetails.clear();
		activeDirectoryDetails.add(details);
	}
}
