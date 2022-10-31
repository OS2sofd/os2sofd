package dk.digitalidentity.sofd.service;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dk.digitalidentity.sofd.dao.FutureEmailsDao;
import dk.digitalidentity.sofd.dao.model.FutureEmail;

@Service
public class FutureEmailsService {
	
	@Autowired
	private FutureEmailsDao futureEmailsDao;

	public List<FutureEmail> getAll() {
		return futureEmailsDao.findAll();
	}
	
	public FutureEmail save(FutureEmail entity) {
		return futureEmailsDao.save(entity);
	}

	public List<FutureEmail> getAllToSend(Date tts) {
		return futureEmailsDao.findByDeliveryTtsBefore(tts);
	}

	public void delete(FutureEmail futureEmail) {
		futureEmailsDao.delete(futureEmail);
	}
}
