package dk.digitalidentity.sofd.controller.api;

import java.io.InputStream;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import dk.digitalidentity.sofd.dao.model.Client;
import dk.digitalidentity.sofd.dao.model.ClientConfig;
import dk.digitalidentity.sofd.security.RequireReadAccess;
import dk.digitalidentity.sofd.security.SecurityUtil;
import dk.digitalidentity.sofd.service.ClientConfigService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequireReadAccess
public class ClientConfigApi {

	@Autowired
	private ClientConfigService clientConfigService;

	@PostMapping("/api/config/upload")
	public ResponseEntity<?> uploadConfig(@RequestParam("file") MultipartFile file) {
		Client client = SecurityUtil.getClient();
		if (client == null) {
			log.error("Could not extract client from request!");
			return new ResponseEntity<>("Unknown client", HttpStatus.FORBIDDEN);
		}

		if (file == null) {
			log.warn("No file supplied for client: " + client.getId());
			return new ResponseEntity<>("No file!", HttpStatus.BAD_REQUEST);			
		}

		try (InputStream is = file.getInputStream()) {
			byte[] content = IOUtils.toByteArray(is);

			ClientConfig clientConfig = clientConfigService.findByClient(client);
			if (clientConfig == null) {
				clientConfig = new ClientConfig();
				clientConfig.setClientId(client.getId());
			}
			
			clientConfig.setClientName(client.getName());
			clientConfig.setConfiguration(content);
			clientConfig.setLastChanged(new Date());

			clientConfigService.save(clientConfig);
		}
		catch (Exception ex) {
			log.error("Failed to parse config file for client: " + client.getId(), ex);
			return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}
}
