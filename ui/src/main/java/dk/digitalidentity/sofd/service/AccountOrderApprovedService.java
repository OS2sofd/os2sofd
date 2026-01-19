package dk.digitalidentity.sofd.service;

import java.time.LocalDateTime;
import java.util.List;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dk.digitalidentity.sofd.dao.AccountOrderApprovedDao;
import dk.digitalidentity.sofd.dao.model.AccountOrderApproved;

@Service
public class AccountOrderApprovedService {

	@Autowired
	private AccountOrderApprovedDao accountOrderApprovedDao;
	
	public List<AccountOrderApproved> findAll() {
		return accountOrderApprovedDao.findAll();
	}
	
	public AccountOrderApproved findById(long id) {
		return accountOrderApprovedDao.findById(id);
	}
	
	public AccountOrderApproved save(AccountOrderApproved approval) {
		return accountOrderApprovedDao.save(approval);
	}
	
	@Transactional
	public void deleteOlderThan13Months() {
		accountOrderApprovedDao.deleteByApprovedTtsBefore(LocalDateTime.now().minusMonths(13));
	}
}
