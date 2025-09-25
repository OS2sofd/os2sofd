package dk.digitalidentity.sofd.controller.api.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacetValueDTO {

	private long facetId;
	private String facetName;
	private String text;
	private String facetListItem;
	private List<String> facetValueOrgunitUuids;
	private String facetValueOrgunitNames;
	private String facetValueAffiliationUuid;
	private String facetValueAffiliationPersonName;
	private LocalDate date;
	private long sortKey;
	
}
