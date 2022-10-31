package dk.digitalidentity.sofd.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dk.digitalidentity.sofd.dao.FunctionDao;
import dk.digitalidentity.sofd.dao.model.Function;

@Service
public class FunctionService {

	@Autowired
	private FunctionDao functionDao;
	
	public List<Function> getAll() {
		return functionDao.findAll();
	}

	public Function getById(long id) {
		return functionDao.findById(id);
	}

	public void delete(Function function) {
		functionDao.delete(function);
	}

	public Function save(Function function) {
		return functionDao.save(function);
	}
}
