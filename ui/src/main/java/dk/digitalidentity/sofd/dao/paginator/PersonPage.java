package dk.digitalidentity.sofd.dao.paginator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.data.jpa.domain.Specification;

import dk.digitalidentity.sofd.dao.model.Person;

public class PersonPage {
	int pageSize;
	Specification<Person> specification;
	Consumer<Person> consumer;
	List<Person> result = new ArrayList<>();
	boolean done = false;
	String offsetUuid = null;

	PersonPage(int pageSize, Specification<Person> specification, Consumer<Person> consumer) {
		this.pageSize = pageSize;
		this.specification = specification;
		this.consumer = consumer;
	}

	public List<Person> getResult() {
		try {
			if (done) {
				throw new RuntimeException("There are no more results - remember to check PersonPager.isDone() before calling getResult()");
			}
			
			if (result == null) {
				throw new RuntimeException("There is no result - did you remember to call PersonPaginator.page() ?");
			}
			
			return result;
		}
		finally {
			result = null;
		}
	}
	
	public boolean isDone() {
		return done;
	}
}
