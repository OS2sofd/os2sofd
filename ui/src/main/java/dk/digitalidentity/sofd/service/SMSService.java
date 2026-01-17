package dk.digitalidentity.sofd.service;

import java.io.IOException;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.service.sms.dto.SendMessageDTO;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SMSService {
	private RestTemplate restTemplate;
	
	@Autowired
	private SofdConfiguration configuration;

	public SMSService() {
		// TODO: use injection - beware we already have a RestTemplate that we need to ensure will work with this
		restTemplate = new RestTemplate();
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {

        	@Override
        	public boolean hasError(ClientHttpResponse response) throws IOException {
        		return false;
        	}
        });
	}

	@Async
	public void sendMessage(String message, Set<String> phoneNumbers) {
		String smsGatewayUrl = configuration.getModules().getSmsGateway().getUrl();
		if (!smsGatewayUrl.endsWith("/")) {
			smsGatewayUrl += "/";
		}

		if (phoneNumbers.size() == 0) {
			return;
		}

		try {
			
			HttpHeaders headers = new HttpHeaders();
			headers.add("Content-Type", "application/json");

			HttpEntity<SendMessageDTO> request = new HttpEntity<SendMessageDTO>(new SendMessageDTO(configuration.getCustomer().getCvr(), message, phoneNumbers), headers);
			
			ResponseEntity<String> response = restTemplate.postForEntity(smsGatewayUrl + "api/gateway", request, String.class);
			if (response.getStatusCode() != HttpStatus.OK) {
				log.error("Failed to send SMS through gateway: " + response.getStatusCodeValue() + " / " + response.getBody());
				return;
			}
		}
		catch (RestClientException ex) {
			log.error("Failed to send SMS through gateway", ex);
		}
	}
}
