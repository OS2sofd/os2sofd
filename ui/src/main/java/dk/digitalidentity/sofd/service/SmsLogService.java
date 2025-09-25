package dk.digitalidentity.sofd.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dk.digitalidentity.sofd.dao.SmsLogDao;
import dk.digitalidentity.sofd.dao.model.SmsLog;

@Service
public class SmsLogService {

	@Autowired
	private SmsLogDao smsLogDao;

	public List<SmsLog> findAll() {
		return smsLogDao.findAll();
	}

	public SmsLog save(SmsLog entity) {
		return smsLogDao.save(entity);
	}

	public Optional<SmsLog> getById(long id) {
		return smsLogDao.getById(id);
	}

}
