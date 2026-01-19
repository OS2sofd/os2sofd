package dk.digitalidentity.sofd.dao.model;

import java.util.List;

import org.hibernate.annotations.BatchSize;

import dk.digitalidentity.sofd.dao.model.mapping.ContactPlaceKleMapping;
import dk.digitalidentity.sofd.dao.model.mapping.ContactPlaceOrgUnitMapping;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
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
