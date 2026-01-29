package dk.digitalidentity.sofd.task;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.dao.model.Client;
import dk.digitalidentity.sofd.dao.model.enums.VersionStatus;
import dk.digitalidentity.sofd.service.AppManagerService;
import dk.digitalidentity.sofd.service.ClientService;
import dk.digitalidentity.sofd.service.model.ApplicationApiDTO;
import dk.digitalidentity.sofd.util.Version;
import lombok.extern.slf4j.Slf4j;

@EnableScheduling
@Component
@Slf4j
public class FetchClientVersionsTask {
	
	@Autowired
	private SofdConfiguration configuration;

	@Autowired
	private AppManagerService appManagerService;

	@Autowired
	private ClientService clientService;

	@Autowired
	private FetchClientVersionsTask self;
	
	// run every hour
	@Scheduled(fixedDelay = 60 * 60 * 1000)
	//@Scheduled(fixedDelay = 10 * 1000)
	public void fetchClientVersions() {
		if (!configuration.getScheduled().isEnabled()) {
			return;
		}

		self.executeTask();
	}

	@Transactional(rollbackOn = Exception.class)
	private void executeTask() {
		List<ApplicationApiDTO> extClients = appManagerService.getApplications();
		
		if (extClients == null) {
			return;
		}
		
		List<Client> dbClients = clientService.findAll().stream().filter(c -> !StringUtils.isBlank(c.getApplicationIdentifier())).collect(Collectors.toList());
		for (Client client : dbClients) {
			
			// find matching client in api call
			ApplicationApiDTO clientDTO = extClients.stream().filter(c -> Objects.equals(c.getIdentifier(), client.getApplicationIdentifier())).findAny().orElse(null);

			if (clientDTO != null) {
				// update client
				client.setNewestVersion(clientDTO.getNewestVersion());
				client.setMinimumVersion(clientDTO.getMinimumVersion());

				if (client.getVersion() != null && client.getNewestVersion() != null && client.getMinimumVersion() != null) {
					if( StringUtils.isBlank(client.getVersion()) ) {
						client.setVersionStatus(VersionStatus.UNKNOWN);
					}
					else {
						Version currentVersion = new Version(client.getVersion());
						Version newestVersion = new Version(client.getNewestVersion());
						Version minimumVersion = new Version(client.getMinimumVersion());

						if (currentVersion.equals(newestVersion)) {
							client.setVersionStatus(VersionStatus.NEWEST);
						}
						else if (currentVersion.compareTo(minimumVersion) < 0) {
							client.setVersionStatus(VersionStatus.OUTDATED);
							log.warn("Client with id " + client.getId() + " and name " + client.getName() + " is running an outdated version of " + client.getApplicationIdentifier());
						}
						else {
							client.setVersionStatus(VersionStatus.UPDATABLE);
						}
					}
				}
				
				clientService.save(client);
			}
			else {
				log.error("Client id: " + client.getId() + " name: " + client.getName() + " has ApplicationIdentifier " + client.getApplicationIdentifier() + " but identifier was not found in API call to AppManager.");
			}			
		}
	}
}
