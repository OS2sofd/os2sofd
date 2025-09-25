package dk.digitalidentity.sofd.service;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dk.digitalidentity.sofd.dao.BatchJobExecutionDao;
import dk.digitalidentity.sofd.dao.model.BatchJobExecution;

@Service
public class BatchJobExecutionService {

	@Autowired
	private BatchJobExecutionDao batchJobExecutionDao;
	
	public List<BatchJobExecution> findAll() {
		return batchJobExecutionDao.findAll();
	}

	public BatchJobExecution save(BatchJobExecution batchJobExecutionEntry) {
		return batchJobExecutionDao.save(batchJobExecutionEntry);
	}

	public void updateExecutionTime(String name, Date executionTime) {
		BatchJobExecution batchJobExecution = batchJobExecutionDao.findByJobName(name);
		
		if (batchJobExecution != null) {
			batchJobExecution.setErrorCount(0);;
			batchJobExecution.setLastExecutionTime(executionTime);
			save(batchJobExecution);
		}
	}

	public long getErrorCount(String name) {
		BatchJobExecution batchJobExecution = batchJobExecutionDao.findByJobName(name);
		
		if (batchJobExecution != null) {
			return batchJobExecution.getErrorCount();
		}
		return 0;
	}

	public void setErrorCount(String name, long errorCount, Date lastErrorTime) {
		BatchJobExecution batchJobExecution = batchJobExecutionDao.findByJobName(name);
		
		if (batchJobExecution != null) {
			batchJobExecution.setErrorCount(errorCount);
			batchJobExecution.setLastErrorTime(lastErrorTime);
			save(batchJobExecution);
		}
	}
	
}
