package dk.digitalidentity.sofd.service;

import static org.apache.commons.io.FilenameUtils.wildcardMatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.io.IOCase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.dao.ProfessionDao;
import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.Profession;
import dk.digitalidentity.sofd.dao.model.enums.ProfessionMatchType;
import dk.digitalidentity.sofd.dao.model.projection.ProfessionLookup;
import dk.digitalidentity.sofd.security.SecurityUtil;
import dk.digitalidentity.sofd.service.model.ProfessionTranslation;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ProfessionService {

	@Autowired
	private ProfessionService self;

	@Autowired
	private ProfessionDao professionDao;

	@Autowired
	private SofdConfiguration configuration;

	@Autowired
    private PersonService personService;

	@Cacheable(value = "professionList")
	public List<Profession> findAllCached() {
		return professionDao.findAll();
	}

	@CacheEvict(value = "professionList", allEntries = true)
	public void resetProfessionCache() {
		;
	}

	@Scheduled(fixedRate = 1 * 60 * 60 * 1000)
	public void resetProfessionCacheTask() {
		self.resetProfessionCache();
	}

	public List<Profession> getByOrganisationId(long organisationId) {
		return professionDao.findByOrganisationId(organisationId);
	}

	public void deleteById(long professionId) {
		professionDao.deleteById(professionId);
	}

	public List<String> getUniquePositionNames(long organisationId) {
		switch (configuration.getModules().getProfessions().getField()) {
			case PAY_GRADE:
				return professionDao.getUniquePayGrades(organisationId);
			case POSITION_NAME:
				return professionDao.getUniquePositionNames(organisationId);
		}

		// fall through default
		return professionDao.getUniquePositionNames(organisationId);
	}

	public Profession save(Profession profession) {
		return professionDao.save(profession);
	}

	public Profession findById(Long id) {
		return id == null ? null : professionDao.findById(id).orElse(null);
	}

	public boolean professionMatchesPosition(Profession profession, String positionName, long organisationId) {
		// ignore professions from other organisations
		if (profession.getOrganisationId() != organisationId) {
			return false;
		}
		
		// first check for negative wildcard matches (for peformance reasons)
		boolean negativeMatch = profession.getProfessionMappings().stream()
				.filter(m -> m.getMatchType() == ProfessionMatchType.NEGATIVE)
				.anyMatch(m -> wildcardMatch(positionName, m.getMatchValue(), IOCase.INSENSITIVE));
		
		if (negativeMatch) {
			return false;
		}

		if (profession.getName().equalsIgnoreCase(positionName)) {
			// match if profession name equals position name
			return true;
		}

		// check for positive wildcard matches
		boolean positiveMatch = profession.getProfessionMappings().stream()
				.filter(m -> m.getMatchType() == ProfessionMatchType.POSITIVE)
				.anyMatch(m -> wildcardMatch(positionName, m.getMatchValue(), IOCase.INSENSITIVE));

		if (positiveMatch) {
			return true;
		}
		
		// check for regex matches
		boolean regexMatch = profession.getProfessionMappings().stream()
				.filter(m -> m.getMatchType() == ProfessionMatchType.REGEX)
				.anyMatch(m -> positionName.matches(m.getMatchValue()));

		if (regexMatch) {
			return true;
		}
		
		return false;
	}

	public void updateAffiliation(Affiliation affiliation) {
		Profession matchingProfession = self.findAllCached().stream()
				.filter(p -> professionMatchesPosition(p, affiliation.getPositionName(), affiliation.getOrgUnit().getBelongsTo().getId()))
				.findFirst()
				.orElse(null);

		affiliation.setProfession(matchingProfession);
	}

	@Transactional
	public void updateAllAffiliations() {
		SecurityUtil.fakeLoginSession();

		self.resetProfessionCache();

		// get all affiliation professions using a fast lookup projection
		List<ProfessionLookup> professionLookup = professionDao.getProfessionLookup();
		log.info("Updating professions on all affiliations");

		long changeCount = 0;
		for (ProfessionLookup lookupAffiliation : professionLookup) {
			final String field = switch (configuration.getModules().getProfessions().getField()) {
				case PAY_GRADE -> {
					yield lookupAffiliation.getPayGrade();
				}
				case POSITION_NAME -> {
					yield lookupAffiliation.getPositionName();
				}
				default -> {
					yield lookupAffiliation.getPositionName();
				}
			};

			Profession matchingProfession = self.findAllCached().stream()
					.filter(p -> professionMatchesPosition(p, field, lookupAffiliation.getOrganisationId()))
					.findFirst()
					.orElse(null);

			boolean changes = false;

			if (matchingProfession == null && lookupAffiliation.getProfessionId() != null) {
				changes = true;
			}
			else if (matchingProfession != null && !Objects.equals(matchingProfession.getId(), lookupAffiliation.getProfessionId())) {
				changes = true;
			}

			if (changes) {
				changeCount++;

				// need to make changes to affiliation - get the real entity from database and update it
				var person = personService.findbyAffiliationId(lookupAffiliation.getAffiliationId());
				if (person != null) {
					var affiliation = person.getAffiliations().stream().filter(a -> a.getId() == lookupAffiliation.getAffiliationId()).findFirst().get();
					affiliation.setProfession(matchingProfession);
					personService.save(person);
				}
				else {
					log.warn("Unable to find person with an affiliation with id " + lookupAffiliation.getAffiliationId());
				}
			}
		}

		log.info("Finished updating professions on all affiliations (" + changeCount + " changes)");
	}

	public List<ProfessionTranslation> getTranslations(long organisationId) {
		List<ProfessionTranslation> translations = new ArrayList<ProfessionTranslation>();
		List<String> allPositionNames = this.getUniquePositionNames(organisationId);
		List<Profession> allProfessions = professionDao.findAll();

		for (String positionName : allPositionNames) {
			List<Profession> matchingProfessions = allProfessions.stream()
					.filter(p -> professionMatchesPosition(p, positionName, organisationId))
					.toList();

			if (matchingProfessions.isEmpty()) {
				ProfessionTranslation translation = new ProfessionTranslation();
				translation.setPositionName(positionName);
				translation.setMessage("Ingen stilling fundet i stillingskataloget");
				translations.add(translation);
			}
			else if (matchingProfessions.size() == 1) {
				ProfessionTranslation translation = new ProfessionTranslation();
				translation.setPositionName(positionName);
				translation.setMessage("Én stilling fundet i stillingskataloget");
				
				String match = matchingProfessions.stream().map(Profession::getName).findFirst().get();
				translation.setTranslation(match);
				translations.add(translation);
			}
			else if (matchingProfessions.size() > 1) {
				ProfessionTranslation translation = new ProfessionTranslation();
				translation.setPositionName(positionName);
				translation.setMessage("Flere fundne stillinger i stillingskataloget");
				
				String matches = matchingProfessions.stream().map(Profession::getName).collect(Collectors.joining(","));
				translation.setTranslation(matches);
				translations.add(translation);
			}
		}

		return translations;
	}

	public boolean professionNameExists(Long organisationId, String name) {
		return professionDao.existsByOrganisationIdAndName(organisationId, name);
	}
}
