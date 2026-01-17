package dk.digitalidentity.sofd.dao.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.Immutable;

import com.fasterxml.jackson.annotation.JsonBackReference;

import dk.digitalidentity.sofd.dao.model.enums.OrgUnitManagerSource;
import dk.digitalidentity.sofd.service.PersonService;
import lombok.Getter;

@Entity(name = "orgunits_manager")
@BatchSize(size = 50)
@Immutable
@Getter
public class OrgUnitManager {

	@Id
	private String orgunitUuid;

	@Column
	private String name;

	// manager_uuid field exposed directly so we don't have to fetch the entire manager entity if we only need the uuid.
	@Formula("manager_uuid")
	private String managerUuid;

	@ManyToOne(fetch = FetchType.LAZY)
	private Person manager;

	@Column
	private boolean inherited;

	@Column
	@Enumerated(EnumType.STRING)
	private OrgUnitManagerSource source;

	@OneToOne
	@JoinColumn(name = "orgunit_uuid")
	@JsonBackReference
	private OrgUnit orgUnit;

	public OrgUnitManager() { }

	// this constructor is used for generating a future manager for UI - not for actually creating a new manager in the db.
	public OrgUnitManager(OrgUnit orgUnit, Person manager, boolean inherited) {
		this.orgUnit = orgUnit;
		this.orgunitUuid = orgUnit.getUuid();
		this.manager = manager;
		this.managerUuid = manager.getUuid();
		this.name = PersonService.getName(manager);
		this.inherited = inherited;
		this.source = OrgUnitManagerSource.SELECTED;
	}
}
