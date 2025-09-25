package dk.digitalidentity.sofd.dao;

import dk.digitalidentity.sofd.dao.model.view.Callcenter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CallcenterDao extends JpaRepository<Callcenter, String>, JpaSpecificationExecutor<Callcenter> {

}
