package dk.digitalidentity.sofd.service;

import dk.digitalidentity.sofd.config.GitConfiguration;
import dk.digitalidentity.sofd.config.SofdConfiguration;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@Slf4j
public class VersionService {

	@Autowired
	SofdConfiguration sofdConfiguration;

	@Autowired
	GitConfiguration gitConfiguration;

	private LocalDateTime firstMissingUpdateTime;

	@Setter
	@Getter
	private String latestVersion;

	public String getVersion() {
		return gitConfiguration.getGitBuildVersion();
	}

	public String getBuildId() {
		return gitConfiguration.getGitCommitId();
	}

	public String getBuildTime() {
		return gitConfiguration.getGitBuildTime();
	}

	public boolean newVersionExists() {
		if( latestVersion == null ) {
			// if latest version hasn't been set yet, never report that a new version is available
			return false;
		}
		else {
			return latestVersion.compareTo(getVersion()) > 0;
		}
	}

	public void verifyAutoUpdateStatus() {
		if( sofdConfiguration.isAutoUpdateEnabled() && newVersionExists() ) {
			var message = "OS2sofd instance is not updated to newest release!";
			// keep track of the first time the missing update is spotted
			if( firstMissingUpdateTime == null ) {
				firstMissingUpdateTime = LocalDateTime.now();
			}
			if(Duration.between(firstMissingUpdateTime,LocalDateTime.now()).toDays() > 5) {
				log.error(message);
			}
			else {
				log.warn(message);
			}
		}
	}
}
