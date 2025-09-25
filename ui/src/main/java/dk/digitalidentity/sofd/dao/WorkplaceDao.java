package dk.digitalidentity.sofd.dao;

import dk.digitalidentity.sofd.dao.model.Workplace;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDate;
import java.util.List;

public interface WorkplaceDao extends CrudRepository<Workplace, Long> {
	List<Workplace> findByStartDate(LocalDate startDate);
	List<Workplace> findByStopDate(LocalDate stopDate);
	@Override
	@NotNull
	List<Workplace> findAll();
}