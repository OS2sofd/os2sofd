package dk.digitalidentity.sofd.service.xls;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.Post;
import dk.digitalidentity.sofd.service.OrgUnitService;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OrgUnitExcelDTO {
	//Organisation (belongs_to.name)
	private String organisation;
	//Uuid
	private String uuid;
	//ParentUuid
	private String parentUuid;
	//Navn
	private String name;
	//Type (orgunit_type)
	private String type;
	//Adresse (street + streetnumber +, postalcode + city from orgunits_posts where prime=1).
	private String address;
	//Cvr
	private String cvr;
	//Ean
	private String ean;
	//SeNr
	private String senr;
	//Pnr
	private String pnr;
	//Sti (the entire path to root org separated by slashes including current orgunit) E.g. "Syddjurs Kommune/Direktion/Direktørområde 3/Afdeling x")
	private String path;

	public OrgUnitExcelDTO(OrgUnit orgUnit) {
		this.organisation = orgUnit.getBelongsTo().getName();
		this.uuid = orgUnit.getUuid();
		this.parentUuid = orgUnit.getParent() == null ? null : orgUnit.getParent().getUuid();
		this.name = orgUnit.getName();
		this.type = orgUnit.getType() != null ? orgUnit.getType().getValue() : "NO TYPE";
		this.address = buildAddress(orgUnit);
		this.cvr = orgUnit.getCvr() != null ? orgUnit.getCvr().toString() : "";
		this.senr = orgUnit.getSenr() != null ? orgUnit.getSenr().toString() : "";
		this.pnr = orgUnit.getPnr() != null ? orgUnit.getPnr().toString() : "";
		this.path = buildPath(orgUnit);
		this.ean = orgUnit.getEanList().stream().map(e -> Long.toString(e.getNumber())).collect(Collectors.joining(", "));
	}

	private String buildPath(OrgUnit orgUnit) {
		OrgUnit p = orgUnit;
		List<String> namsd = new ArrayList<>();
		while (p != null) {
			namsd.add(p.getName());
			p = p.getParent();
		}
		Collections.reverse(namsd);

		return String.join("/", namsd);
	}

	//Maybe this should be in OrgUnitService
	private String buildAddress(OrgUnit orgUnit) {
		// find prime post address
		Optional<Post> post = OrgUnitService.getPosts(orgUnit).stream().filter(p -> p.isPrime()).findFirst();
		String tmpPostValue = null;
		if (post.isPresent()) {
			Post p = post.get();
			tmpPostValue = p.getStreet() + ", " + p.getPostalCode() + " " + p.getCity();
		}
		return tmpPostValue;
	}
}
