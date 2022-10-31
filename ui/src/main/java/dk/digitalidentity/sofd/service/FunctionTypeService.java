package dk.digitalidentity.sofd.service;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dk.digitalidentity.sofd.controller.mvc.dto.FunctionTypeDTO;
import dk.digitalidentity.sofd.dao.FunctionTypeDao;
import dk.digitalidentity.sofd.dao.model.FunctionType;

@Service
public class FunctionTypeService {
	private static FunctionTypeService instance;

	public static FunctionTypeService getInstance() {
		return instance;
	}
	
	@Autowired
	private FunctionTypeDao functionTypeDao;
	
	@PostConstruct
	public void init() {
		instance = this;
	}
	
	public List<FunctionType> findAll() {
		return functionTypeDao.findAll();
	}
	
	public List<FunctionTypeDTO> findAllAsDTO() {
		return functionTypeDao.findAll().stream().map(f -> new FunctionTypeDTO(f)).collect(Collectors.toList());
	}

	public FunctionType findById(long id) {
		return functionTypeDao.findById(id);
	}

	public FunctionType save(FunctionType functionType) {
		return functionTypeDao.save(functionType);		
	}
	
	public void delete(FunctionType functionType) {
		functionTypeDao.delete(functionType);
	}
}
