package dk.digitalidentity.sofd.telephony.dao;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.history.RevisionRepository;

import dk.digitalidentity.sofd.telephony.dao.model.TelephonyHistoryView;
import dk.digitalidentity.sofd.telephony.dao.model.TelephonyPhone;

public interface TelephonyPhoneDao extends CrudRepository<TelephonyPhone, Long>, RevisionRepository<TelephonyPhone, Long, Integer> {
	List<TelephonyPhone> findAll();

	TelephonyPhone findById(long id);

	@Query(nativeQuery = true, value = "SELECT " +
			"   t.phone_number, " +
			"   SUBSTR(FROM_UNIXTIME(r.timestamp / 1000), 1,10) AS `timestamp`, " +
			"   COALESCE(NULLIF(p.chosen_name, ''), CONCAT(p.firstname, ' ', p.surname)) AS name " + 
			" FROM telephony_phones_aud t" + 
			" LEFT JOIN persons p ON t.person_uuid = p.uuid" + 
			" JOIN revisions r ON t.rev = r.id" + 
			" WHERE t.phone_number = ?1")
	List<TelephonyHistoryView> findHistoricPhoneHoldersByPhoneNumber(String query);
	
	@Query(nativeQuery = true, value = "SELECT MAX(rev) FROM telephony_phones_aud")
	Long getMaxRev();
	
	@Query(nativeQuery = true, value = "SELECT t.*" + 
			"  FROM telephony_phones_aud ta" + 
			"  JOIN telephony_phones t ON t.id = ta.id" + 
			"  WHERE ta.rev > ?1 and ta.rev <= ?2" + 
			"  GROUP BY ta.id")
	List<TelephonyPhone> getChangesSince(long lastRevId, long maxRevId);
}
