package dk.digitalidentity.sofd.config;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.info.Info.Builder;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import dk.digitalidentity.sofd.dao.model.Client;
import dk.digitalidentity.sofd.service.ClientService;

@Component
@PropertySource("classpath:git.properties")
public class BuildInfoContributor implements InfoContributor {
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

	@Autowired
	private ClientService clientService;

	@Value(value = "${git.build.time}")
	private String gitBuildTime;

	@Override
	public void contribute(Builder builder) {
		synchronized (sdf) {
			List<Client> clients = clientService.findAll();

			builder.withDetail("buildTime", gitBuildTime);
			builder.withDetail("clients", clients.stream().filter(c -> c.isShowOnFrontpage()).map(c -> new ClientInfo(c)).collect(Collectors.toList()));			
		}
	}
	
	class ClientInfo {
		public String name;
		public String version;
		public String lastActive;
		
		public ClientInfo(Client client) {
			name = client.getName();
			version = client.getVersion();
			lastActive = client.getLastActive() != null ? sdf.format(client.getLastActive()) : "";
		}
	}
}
