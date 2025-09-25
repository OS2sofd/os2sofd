package dk.digitalidentity.sofd.dao;

import dk.digitalidentity.sofd.dao.model.SchoolUser;
import dk.digitalidentity.sofd.dao.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SchoolUserDao extends JpaRepository<User, Long> {

	@Query(nativeQuery = true, value = """
			select
				u.id,
				ad.kombit_uuid as uuid,
				u.user_id as userId,
				u.disabled,
				ifnull(p.chosen_name,concat(p.firstname,' ',p.surname)) as name,
				ad.title,
				p.cpr,
				u.local_extensions as localExtensions
			from users u
			inner join active_directory_details ad on ad.user_id = u.id
			inner join persons_users pu on pu.user_id = u.id
			inner join persons p on p.uuid = pu.person_uuid
			where
				u.user_type = 'ACTIVE_DIRECTORY_SCHOOL'
				and u.id > :offset order by id limit :size
			""")
	List<SchoolUser> findLimitedWithOffset(int size, long offset);
}