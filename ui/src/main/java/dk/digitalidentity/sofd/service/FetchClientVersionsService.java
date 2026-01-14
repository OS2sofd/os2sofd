package dk.digitalidentity.sofd.service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import dk.digitalidentity.sofd.dao.model.Client;
import dk.digitalidentity.sofd.dao.model.enums.VersionStatus;
import dk.digitalidentity.sofd.service.model.ApplicationApiDTO;
import dk.digitalidentity.sofd.util.Version;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class FetchClientVersionsService {

	@Autowired
	private AppManagerService appManagerService;

	@Autowired
	private ClientService clientService;

	@Transactional
	public void executeTask() {
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
