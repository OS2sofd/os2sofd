package dk.digitalidentity.sofd.controller.api.v2.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonFormat;

import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.OrgUnitTag;
import dk.digitalidentity.sofd.dao.model.Phone;
import dk.digitalidentity.sofd.dao.model.Post;
import dk.digitalidentity.sofd.dao.model.mapping.OrgUnitPhoneMapping;
import dk.digitalidentity.sofd.dao.model.mapping.OrgUnitPostMapping;
import dk.digitalidentity.sofd.service.OrgUnitService;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(exclude = { "localExtensions" })
@NoArgsConstructor
public class OrgUnitApiRecord extends BaseRecord {

	// primary key
	@Pattern(regexp = "([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})", message = "Invalid uuid")
	private String uuid;

	// read/write fields

	@NotNull
	private String master;

	@NotNull
	private String masterId;

	@NotNull
	private String shortname;

	@NotNull
	private String name;

	private String displayName;
	private Long cvr;
	private String cvrName;
	private Long ean;
	private Long senr;
	private Long pnr;
	private String costBearer;
	private String orgType;
	private Long orgTypeId;
	private String parentUuid;
	private Map<String, Object> localExtensions;
	private Boolean deleted;
	private String email;
	private String urlAddress;

	@Valid
	private Set<PostApiRecord> postAddresses;

	@Valid
	private Set<PhoneApiRecord> phones;

	// readonly

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime created;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime lastChanged;

	private ManagerApiRecord manager;

	private Set<OrgUnitTagApiRecord> tags;

	private long id;

	public OrgUnitApiRecord(OrgUnit orgUnit) {
		this.uuid = orgUnit.getUuid();
		this.master = orgUnit.getMaster();
		this.masterId = orgUnit.getMasterId();
		this.created = (orgUnit.getCreated() != null) ? toLocalDateTime(orgUnit.getCreated()) : null;
		this.lastChanged = (orgUnit.getLastChanged() != null) ? toLocalDateTime(orgUnit.getLastChanged()) : null;
		this.deleted = orgUnit.isDeleted();
		this.shortname = orgUnit.getShortname();
		this.name = orgUnit.getSourceName();
		this.displayName = orgUnit.getDisplayName();
		this.cvr = orgUnit.getCvr();
		this.cvrName = orgUnit.getCvrName();
		this.ean = orgUnit.getEan();
		this.senr = orgUnit.getSenr();
		this.pnr = orgUnit.getPnr();
		this.costBearer = orgUnit.getCostBearer();
		this.orgType = orgUnit.getOrgType();
		this.orgTypeId = orgUnit.getOrgTypeId();
		this.localExtensions = stringToMap(orgUnit.getLocalExtensions());
		this.parentUuid = (orgUnit.getParent() != null) ? orgUnit.getParent().getUuid() : null;
		this.id = orgUnit.getId();
		this.email = orgUnit.getEmail();
		this.urlAddress = orgUnit.getUrlAddress();

		if (orgUnit.getManager() != null) {
			this.manager = new ManagerApiRecord();
			this.manager.setName(orgUnit.getManager().getName());
			this.manager.setUuid(orgUnit.getManager().getManagerUuid());
			this.manager.setInherited(orgUnit.getManager().isInherited());
		}

		if (orgUnit.getPhones() != null) {
			this.phones = new HashSet<PhoneApiRecord>();

			for (Phone phone : OrgUnitService.getPhones(orgUnit)) {
				this.phones.add(new PhoneApiRecord(phone));
			}
		}

		if (orgUnit.getPostAddresses() != null) {
			this.postAddresses = new HashSet<PostApiRecord>();

			for (Post post : OrgUnitService.getPosts(orgUnit)) {
				this.postAddresses.add(new PostApiRecord(post));
			}
		}

		if( orgUnit.getTags() != null) {
			this.tags = new HashSet<OrgUnitTagApiRecord>();

			for (OrgUnitTag tag : orgUnit.getTags()) {

				this.tags.add(new OrgUnitTagApiRecord(tag));
			}
		}
	}

	public OrgUnit toOrgUnit(OrgUnit actualOrgUnit) {
		OrgUnit orgUnit = new OrgUnit();

		if (actualOrgUnit == null) {
			actualOrgUnit = orgUnit;
		}

		orgUnit.setLocalExtensions(mapToString(localExtensions));
		orgUnit.setMaster(master);
		orgUnit.setMasterId(masterId);
		orgUnit.setCostBearer(costBearer);
		orgUnit.setCvr(cvr);
		orgUnit.setCvrName(cvrName);
		orgUnit.setEan(ean);
		orgUnit.setSourceName(name);
		orgUnit.setOrgType(orgType);
		orgUnit.setOrgTypeId(orgTypeId);
		orgUnit.setPnr(pnr);
		orgUnit.setSenr(senr);
		orgUnit.setShortname(shortname);
		orgUnit.setDisplayName(displayName);
		orgUnit.setUuid(uuid);
		orgUnit.setDeleted((deleted != null) ? deleted : false);
		orgUnit.setEmail(email);
		orgUnit.setUrlAddress(urlAddress);

		if (parentUuid != null) {
			OrgUnit ou = OrgUnitService.getInstance().getByUuid(parentUuid);
			if (ou != null) {
				orgUnit.setParent(ou);
			}
		}

		if (postAddresses != null) {
			orgUnit.setPostAddresses(new ArrayList<>());

			for (PostApiRecord postRecord : postAddresses) {
				OrgUnitPostMapping mapping = new OrgUnitPostMapping();
				mapping.setPost(postRecord.toPost());
				mapping.setOrgUnit(actualOrgUnit);

				orgUnit.getPostAddresses().add(mapping);
			}
		}

		if (phones != null) {
			orgUnit.setPhones(new ArrayList<>());

			for (PhoneApiRecord phoneRecord : phones) {
				OrgUnitPhoneMapping mapping = new OrgUnitPhoneMapping();
				mapping.setPhone(phoneRecord.toPhone());
				mapping.setOrgUnit(actualOrgUnit);

				orgUnit.getPhones().add(mapping);
			}
		}

		if (manager != null) {
			orgUnit.setManager(manager.toOrgUnitManager());
		}

		return orgUnit;
	}
}
