package dk.digitalidentity.sofd.controller.mvc.xls;

import java.util.ArrayList;
import java.util.List;

import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.OrgUnitAccountOrder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountOrderRulesXlsDto {
	private List<Pair> rulePairs = new ArrayList<>();
	
	public List<Pair> getRulePairs() {
		return rulePairs;
	}
	
	public void add(OrgUnit orgUnit, OrgUnitAccountOrder order) {
		Pair pair = new Pair();
		pair.order = order;
		pair.orgUnit = orgUnit;
		
		rulePairs.add(pair);
	}

	class Pair {
		OrgUnitAccountOrder order;
		OrgUnit orgUnit;
	}
}
