package dk.digitalidentity.sofd.dao;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import dk.digitalidentity.sofd.dao.model.Setting;

public interface SettingDao extends CrudRepository<Setting, Long> {
	Setting findById(long id);
	Setting findByKey(String key);
	
	List<Setting> findAll();
}
