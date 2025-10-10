package dk.digitalidentity.sofd.dao;

import dk.digitalidentity.sofd.dao.model.Person;
import java.util.List;

public interface PersonDaoCustom {
	List<Person> findPersonsWithDuplicateUsers(Person person, String personUuid);
}