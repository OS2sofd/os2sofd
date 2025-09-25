package dk.digitalidentity.sofd.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@PropertySource("classpath:git.properties")
public class GitConfiguration {
    @Value(value = "${git.build.version}")
    private String gitBuildVersion;

    @Value(value = "${git.build.time}")
    private String gitBuildTime;

    @Value(value = "${git.commit.id.abbrev}")
    private String gitCommitId;
}
