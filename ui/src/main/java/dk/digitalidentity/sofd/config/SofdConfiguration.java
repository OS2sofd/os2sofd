package dk.digitalidentity.sofd.config;

import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import dk.digitalidentity.sofd.config.properties.AmazonS3Details;
import dk.digitalidentity.sofd.config.properties.Customer;
import dk.digitalidentity.sofd.config.properties.EmailTemplateConfiguration;
import dk.digitalidentity.sofd.config.properties.Integrations;
import dk.digitalidentity.sofd.config.properties.Modules;
import dk.digitalidentity.sofd.config.properties.Scheduled;
import lombok.Getter;
import lombok.Setter;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "sofd")
public class SofdConfiguration {
	private String version = "2022 r1";
	private String latestVersion = "2022 r1";
	
	private Modules modules = new Modules();
	private Customer customer = new Customer();
	private Integrations integrations = new Integrations();
	private Scheduled scheduled = new Scheduled();
	private AmazonS3Details s3 = new AmazonS3Details();
	private EmailTemplateConfiguration emailTemplate = new EmailTemplateConfiguration();
	private boolean hidePersonAddresses = false; // TODO: burde den ikke defaulte til true?

	public boolean checkVersion() {
		return Objects.equals(version, latestVersion);
	}
}
