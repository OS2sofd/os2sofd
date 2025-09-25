package dk.digitalidentity.sofd.dao;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import dk.digitalidentity.sofd.dao.model.FunctionType;

public interface FunctionTypeDao extends CrudRepository<FunctionType, Long> {
    List<FunctionType> findAll();

    FunctionType findById(long id);
}