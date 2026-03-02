package dk.digitalidentity.sofd.dao.model;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.BatchSize;

import dk.digitalidentity.sofd.dao.model.mapping.ContactPlaceKleMapping;
import dk.digitalidentity.sofd.dao.model.mapping.ContactPlaceOrgUnitMapping;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "contactplaces")
@Getter
@Setter
public class ContactPlace {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@OneToOne
	@JoinColumn(name = "orgunit_uuid", referencedColumnName = "uuid")
	private OrgUnit contactPlace;

	@BatchSize(size = 100)
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "contactPlace")
	private List<ContactPlaceKleMapping> tasks;
	
	@BatchSize(size = 100)
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "contactPlace")
	private List<ContactPlaceOrgUnitMapping> users;
	
	@Column
	private boolean synchronizedToOrganisation;
	
	@Column
	private boolean deleted;
}
