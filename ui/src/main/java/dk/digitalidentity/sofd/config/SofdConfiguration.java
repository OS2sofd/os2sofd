package dk.digitalidentity.sofd.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dk.digitalidentity.sofd.config.properties.AmazonS3Details;
import dk.digitalidentity.sofd.config.properties.Customer;
import dk.digitalidentity.sofd.config.properties.EmailTemplateConfiguration;
import dk.digitalidentity.sofd.config.properties.Integrations;
import dk.digitalidentity.sofd.config.properties.Modules;
import dk.digitalidentity.sofd.config.properties.Scheduled;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "sofd")
public class SofdConfiguration {
	private boolean autoUpdateEnabled = false;
	private Modules modules = new Modules();
	private Customer customer = new Customer();
	private Integrations integrations = new Integrations();
	private Scheduled scheduled = new Scheduled();
	private EmailTemplateConfiguration emailTemplate = new EmailTemplateConfiguration();
	
	@JsonIgnore
	private AmazonS3Details s3 = new AmazonS3Details();
		
	private boolean hidePersonAddresses = false; // TODO: burde den ikke defaulte til true?

	@FeatureDocumentation(name = "Funktionsoverblik", description = "Vis tillidsfunktioner indlæst fra OPUS")
	private boolean showOpusFunctionsOnPersonPage = false;
}
