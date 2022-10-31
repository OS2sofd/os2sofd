package dk.digitalidentity.sofd.dao;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import dk.digitalidentity.sofd.dao.model.SOFDAccount;

public interface SOFDAccountDao extends CrudRepository<SOFDAccount, Long> {
	List<SOFDAccount> findAll();

	SOFDAccount findById(long id);

	SOFDAccount findByUserId(String userId);
}