package dk.digitalidentity.sofd.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dk.digitalidentity.sofd.dao.TagsDao;
import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.OrgUnitTag;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.Tag;

@Service
public class TagsService {

	@Autowired
	private TagsDao tagsDao;
	
	public List<Tag> findAll() {
		return tagsDao.findAll();
	}

	public Tag findByValue(String tag) {
		return tagsDao.findByValue(tag);
	}
	
	public Tag findById(Long id) {
		return tagsDao.findById(id).orElse(null);
	}

	public Tag save(Tag tag) {
		return tagsDao.save(tag);
	}

	public void delete(Long id) {
		tagsDao.deleteById(id);
	}

	public boolean existsByValue(String value) {
		return tagsDao.existsByValue(value);
	}

	public String getTagValueForPersonsPrimaryAffiliation(Person person, String tagValue) {
		Tag tag = tagsDao.findByValue(tagValue);
		if (tag != null) {
			Affiliation affiliation = person.getAffiliations().stream().filter(a -> a.isPrime()).findFirst().orElse(null);
			if (affiliation != null) {
				OrgUnitTag ouTag = affiliation.getOrgUnit().getTags().stream().filter(t -> t.getTag().getId() == tag.getId()).findFirst().orElse(null);
				if (ouTag != null) {
					return ouTag.getCustomValue();
				}
			}
		}
		
		return null;
	}
}
