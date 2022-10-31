package dk.digitalidentity.sofd.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import dk.digitalidentity.sofd.dao.CallcenterDao;
import dk.digitalidentity.sofd.dao.model.view.Callcenter;

@Service
public class CallcenterService {

    @Autowired
    private CallcenterDao callcenterDao;

    public List<Callcenter> getBySearch(String[] searchTerms) {

    	// create where clause
        Specification<Callcenter> spec = null;
        boolean first = true;
        for (String term : searchTerms) {
            spec = first ? Specification.where(like(term)) : spec.and(like(term));
            first = false;
        }

        // find and return all matching
        return callcenterDao.findAll(spec);
    }

    private static Specification<Callcenter> like(String term) {
        return Specification
                .where(nameLike(term))
                .or(orgUnitLike(term))
                .or(userIdLike(term))
                .or(keywordsLike(term))
                .or(phoneLike(term))
                .or(phoneNumbersLike(term))
                .or(emailLike(term))
                .or(addressLike(term))
                .or(positionNameLike(term));
    }

    private static Specification<Callcenter> nameLike(String term) {
        return (root, query, cb) -> term == null ? null : cb.like(root.get("name"), ("%" + term + "%"));
    }

    private static Specification<Callcenter> orgUnitLike(String term) {
        return (root, query, cb) -> term == null ? null : cb.like(root.get("orgUnit"), ("%" + term + "%"));
    }

    private static Specification<Callcenter> userIdLike(String term) {
        return (root, query, cb) -> term == null ? null : cb.like(root.get("userId"), ("%" + term + "%"));
    }

    private static Specification<Callcenter> keywordsLike(String term) {
        return (root, query, cb) -> term == null ? null : cb.like(root.get("keywords"), ("%" + term + "%"));
    }

    private static Specification<Callcenter> phoneLike(String term) {
        return (root, query, cb) -> term == null ? null : cb.like(root.get("phone"), ("%" + term + "%"));
    }

    private static Specification<Callcenter> phoneNumbersLike(String term) {
        return (root, query, cb) -> term == null ? null : cb.like(root.get("phoneNumbers"), ("%" + term + "%"));
    }

    private static Specification<Callcenter> emailLike(String term) {
        return (root, query, cb) -> term == null ? null : cb.like(root.get("email"), ("%" + term + "%"));
    }

    private static Specification<Callcenter> addressLike(String term) {
        return (root, query, cb) -> term == null ? null : cb.like(root.get("address"), ("%" + term + "%"));
    }

    private static Specification<Callcenter> positionNameLike(String term) {
        return (root, query, cb) -> term == null ? null : cb.like(root.get("positionName"), ("%" + term + "%"));
    }
}
