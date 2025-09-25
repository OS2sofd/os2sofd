package dk.digitalidentity.sofd.dao;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import dk.digitalidentity.sofd.dao.model.User;

public interface UserDao extends CrudRepository<User, Long> {
	User findByUserIdAndUserType(String userId, String userType);
	User findByUserIdAndUserTypeAndMaster(String userId, String userType, String master);
	User findByUuid(String uuid);
	List<User> findAll();
	List<User> findByUserIdLikeAndUserType(String word, String userType);
	List<User> findByUserTypeAndDisabledFalse(String userType);
}