package dk.digitalidentity.sofd.dao.paginator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.digitalidentity.sofd.dao.model.Person;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class PersonPaginator {
	private final static int PAGE_SIZE = 1000;
    private final EntityManager entityManager;
	
	public PersonPage initPaginator(int pageSize, Specification<Person> specification, Consumer<Person> consumer) {
		return new PersonPage(pageSize, specification, consumer);
	}
	
	@Transactional(readOnly = true)
	public void page(PersonPage pager) {
		if (pager.done) {
			pager.result = null;
			return;
		}

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Person> query = cb.createQuery(Person.class);
        Root<Person> root = query.from(Person.class);

        List<Predicate> predicates = new ArrayList<>();
        if (pager.specification != null) {
        	predicates.add(pager.specification.toPredicate(root, query, cb));
        }

		if (pager.offsetUuid != null) {
            predicates.add(cb.greaterThan(
            	root.get("uuid"),
            	pager.offsetUuid
            ));
		}

		if (predicates.size() > 0) {
            query.where(cb.and(predicates.toArray(new Predicate[0])));
		}
		
        List<Person> result = entityManager
    		.createQuery(query)
            .setMaxResults(pager.pageSize)
            .getResultList();

        if (pager.consumer != null) {
        	result.forEach(pager.consumer);
        }

        if (result.size() == 0) {
        	pager.done = true;
        	pager.result = null;
        }
        else {
            pager.result = result;
        	pager.offsetUuid = result.stream().map(p -> p.getUuid()).max(String::compareTo).orElse(null);
        }
	}

	// init-method variants with default arguments

	public PersonPage initPaginator() {
		return initPaginator(PAGE_SIZE, null, null); 
	}
	
	public PersonPage initPaginator(int pageSize) {
		return initPaginator(pageSize, null, null); 
	}

	public PersonPage initPaginator(int pageSize, Consumer<Person> consumer) {
		return initPaginator(pageSize, null, consumer); 
	}

	public PersonPage initPaginator(Specification<Person> specification) {
		return initPaginator(PAGE_SIZE, specification, null); 
	}

	public PersonPage initPaginator(Specification<Person> specification, Consumer<Person> consumer) {
		return initPaginator(PAGE_SIZE, specification, consumer); 
	}

	public PersonPage initPaginator(Consumer<Person> consumer) {
		return initPaginator(PAGE_SIZE, null, consumer); 
	}
}
