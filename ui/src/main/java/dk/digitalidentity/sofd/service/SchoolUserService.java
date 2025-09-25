package dk.digitalidentity.sofd.service;

import dk.digitalidentity.sofd.dao.SchoolUserDao;
import dk.digitalidentity.sofd.dao.model.SchoolUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SchoolUserService {

	@Autowired
	private SchoolUserDao schoolUserDao;

	public List<SchoolUser> getByOffsetAndLimit(long offset, int size) {
		return schoolUserDao.findLimitedWithOffset(size, offset);
	}
}