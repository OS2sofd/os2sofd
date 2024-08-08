package dk.digitalidentity.sofd.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dk.digitalidentity.sofd.dao.EanDao;
import dk.digitalidentity.sofd.dao.model.Ean;
import dk.digitalidentity.sofd.dao.model.OrgUnit;

@Service
public class EanService {

	@Autowired
	private EanDao eadDao;

	public Ean findById(long id) {
		return eadDao.findById(id);
	}

	public String getEan(OrgUnit orgUnit) {
		Long eanValue = null;
		OrgUnit cursor = orgUnit;
		do {
			if (cursor.getEanList() == null || cursor.getEanList().isEmpty() || cursor.getEanList().stream().noneMatch(Ean::isPrime)) {
				cursor = cursor.getParent();
			} else {
				eanValue = cursor.getEanList().stream().filter(Ean::isPrime).findAny().get().getNumber();
				break;
			}
		} while (cursor != null);

		return eanValue != null ? Long.toString(eanValue) : null;
	}

}
