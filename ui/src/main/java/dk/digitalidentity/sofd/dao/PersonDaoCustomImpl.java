package dk.digitalidentity.sofd.dao;

import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.mapping.PersonUserMapping;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.Collections;
import java.util.List;

@Component
public class PersonDaoCustomImpl implements PersonDaoCustom {

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public List<Person> findPersonsWithDuplicateUsers(Person person, String personUuid) {
		if (person == null || person.getUsers() == null || person.getUsers().isEmpty()) {
			return Collections.emptyList();
		}

		StringBuilder sql = new StringBuilder("""
            SELECT p.*
            FROM persons p
            INNER JOIN persons_users pu ON pu.person_uuid = p.uuid
            INNER JOIN users u ON u.id = pu.user_id
            WHERE p.uuid <> ?
            AND (u.master, u.master_id) IN (
            """);

		// build placeholders for each composite key pair
		for (int i = 0; i < person.getUsers().size(); i++) {
			if (i > 0) {
				sql.append(",");
			}
			sql.append("(?,?)");
		}
		sql.append(")");

		Query query = entityManager.createNativeQuery(sql.toString(), Person.class);
		query.setFlushMode(FlushModeType.COMMIT); // don't flush before this query

		// set uuid parameter - empty string instead of null for sql syntax reasons (<> null does not give valid results))
		query.setParameter(1, personUuid == null ? "" : personUuid);

		// set composite key parameters
		int paramIndex = 2;
		for (PersonUserMapping user : person.getUsers()) {
			query.setParameter(paramIndex++, user.getUser().getMaster());
			query.setParameter(paramIndex++, user.getUser().getMasterId());
		}

		return query.getResultList();
	}
}