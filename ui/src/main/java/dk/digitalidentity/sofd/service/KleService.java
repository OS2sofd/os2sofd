package dk.digitalidentity.sofd.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.http.HttpEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.dao.KleDao;
import dk.digitalidentity.sofd.dao.model.Kle;
import dk.digitalidentity.sofd.service.model.KleDtoWrapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@EnableCaching
@Service
public class KleService {
	private Map<String, String> kleCacheMap = new HashMap<>();

	@Autowired
	private KleDao kleDao;
	
	@Autowired
	private KleService self;
	
	@Autowired
	private SofdConfiguration configuration;

	@Qualifier("defaultRestTemplate")
	@Autowired
	private RestTemplate restTemplate;

	@Cacheable(value = "kleList")
	public List<Kle> findAll() {
		return kleDao.findAll();
	}
	
	// 4 hour cache should be enough to ensure solid performance
	@Scheduled(fixedRate = 4 * 60 * 60 * 1000)
	public void resetKleCacheTask() {
		self.resetKleCache();
	}

	@CacheEvict(value = "kleList", allEntries = true)
	public void resetKleCache() {
		;
	}

	public List<Kle> findAllByParent(String parent) {
		return kleDao.findAllByParent(parent);
	}

	public Kle getByCode(String code) {
		return kleDao.getByCode(code);
	}

	public long countByActiveTrue() {
		return kleDao.countByActiveTrue();
	}

	public Kle save(Kle kle) {
		return kleDao.save(kle);
	}
	
	public Collection<Kle> loadKleFromKOMBIT(String cvr) {
		Map<String, Kle> kleMap = new HashMap<>();

		HttpEntity<KleDtoWrapper> responseRootEntity = restTemplate.getForEntity(configuration.getIntegrations().getKle().getUrl() + "/odata/Klasser?$filter=KlasseTilhoerer eq '00000c7e-face-4001-8000-000000000000' and Cvr eq '" + cvr + "'&$select=UUID,BrugervendtNoegle,Titel,Tilstand", KleDtoWrapper.class);

		for (dk.digitalidentity.sofd.service.model.KleDto kleDto : responseRootEntity.getBody().getContent()) {
			Kle kle = new Kle();
			kle.setCode(kleDto.getCode());
			kle.setName(kleDto.getTitle());
			kle.setActive(kleDto.isActive());
			kle.setUuid(kleDto.getUuid());

			if (kleDto.getCode().length() == 2) {
				kle.setParent("0");
			}
			else if (kleDto.getCode().length() == 5) {
				kle.setParent(kleDto.getCode().substring(0, 2));
			}
			else if (kleDto.getCode().length() == 8) {
				kle.setParent(kleDto.getCode().substring(0, 5));
			} else {
				log.warn("Invalid KLE: " + kleDto.getCode());
				continue;
			}

			if (kleMap.containsKey(kle.getCode())) {
				Kle otherKle = kleMap.get(kle.getCode());
				if (otherKle.isActive() == kle.isActive()) {
					log.warn("KLE with code " + kle.getCode() + " is in the set from KOMBIT twice with same status");
				}
				else if (!otherKle.isActive()) {
					// overwrite with active version (KOMBIT bug, they are keeping multple versions instead of updating the actual version)
					kleMap.put(kle.getCode(), kle);
				}
			}
			else {
				kleMap.put(kle.getCode(), kle);
			}
		}

		return kleMap.values();
	}
	
	public String getName(String code) {
		return kleCacheMap.get(code);
	}
	
	public void updateCache() {
		if (!configuration.getScheduled().isEnabled()) {
			return;
		}

		if (!StringUtils.hasLength(configuration.getIntegrations().getKle().getUrl())) {
			log.warn("KLE url empty, not fetching updated KLE data");
			return;
		}

		log.info("Fetching KLE from KOMBIT and refreshing cache");

		Map<String, String> newCacheMap = new HashMap<>();
		Collection<Kle> updatedKleList = self.loadKleFromKOMBIT(configuration.getCustomer().getCvr());
		log.info("Loaded KLE from Kombit. Size: " + updatedKleList.size());
		List<Kle> kleList = self.findAll();

		for (Kle updatedKle : updatedKleList) {
			boolean found = false;
			boolean changes = false;

			for (Iterator<Kle> iterator = kleList.iterator(); iterator.hasNext(); ) {
				Kle kle = iterator.next();

				if (kle.getCode().equals(updatedKle.getCode())) {
					found = true;

					if (!Objects.equals(kle.getName(), updatedKle.getName())) {
						changes = true;
					}

					if (kle.isActive() != updatedKle.isActive()) {
						changes = true;
					}
					
					if (!Objects.equals(kle.getUuid(), updatedKle.getUuid())) {
						changes = true;
					}

					iterator.remove();
					break;
				}
			}

			if (found && changes) {
				// fetch from DB to bypass cache
				Kle kle = self.getByCode(updatedKle.getCode());
				kle.setName(updatedKle.getName());
				kle.setActive(updatedKle.isActive());
				kle.setUuid(updatedKle.getUuid());

				self.save(kle);
			}
			else if (!found) {
				self.save(updatedKle);
			}

			newCacheMap.put(updatedKle.getCode(), updatedKle.getName());
		}

		// Deactivate whatever is left in the list
		for (Kle inactiveKle : kleList) {
			Kle kle = self.getByCode(inactiveKle.getCode());
			kle.setActive(false);

			self.save(kle);
		}

		kleCacheMap = newCacheMap;
		log.info("Finished fetching KLE from KOMBIT and refreshing cache");
	}
	
	// do not reload cache on the instance that is running the scheduled task
	public void reloadCache(boolean force) {
		if (!force && configuration.getScheduled().isEnabled()) {
			return;
		}

		log.info("Refreshing KLE cache");

		loadCache();

		log.info("Finished refreshing KLE cache");
	}
	
	private void loadCache() {
		Map<String, String> newKleCacheMap = new HashMap<>();

		self.resetKleCache();
		List<Kle> kleList = self.findAll();
		for (Kle kle : kleList) {
			newKleCacheMap.put(kle.getCode(), kle.getName());
		}

		kleCacheMap = newKleCacheMap;
	}
}
