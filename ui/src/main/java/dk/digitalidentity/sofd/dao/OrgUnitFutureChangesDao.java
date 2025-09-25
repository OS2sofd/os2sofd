package dk.digitalidentity.sofd.dao;

import java.util.Date;
import java.util.List;

import org.springframework.data.repository.CrudRepository;

import dk.digitalidentity.sofd.dao.model.OrgUnitFutureChange;
import dk.digitalidentity.sofd.dao.model.enums.AppliedStatus;

public interface OrgUnitFutureChangesDao extends CrudRepository<OrgUnitFutureChange, Long> {
    long countByAppliedStatus(AppliedStatus appliedStatus);
    List<OrgUnitFutureChange> findAll();
    List<OrgUnitFutureChange> findAllByAppliedStatus(AppliedStatus appliedStatus);
    List<OrgUnitFutureChange> findAllByChangeDateLessThanEqualAndAppliedStatus(Date changeDate, AppliedStatus appliedStatus);
    List<OrgUnitFutureChange> findAllByOrgunitUuidAndAppliedStatus(String uuid, AppliedStatus appliedStatus);
    List<OrgUnitFutureChange> findAllByOrgunitUuidAndChangeDateLessThanEqualAndAppliedStatus(String uuid, Date changeDate, AppliedStatus appliedStatus);
    List<OrgUnitFutureChange> findAllByOrgunitUuidInAndChangeDateLessThanEqualAndAppliedStatus(List<String> uuids, Date changeDate, AppliedStatus appliedStatus);
	OrgUnitFutureChange findById(long id);
}