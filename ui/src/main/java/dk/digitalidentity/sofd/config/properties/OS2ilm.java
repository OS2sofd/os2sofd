package dk.digitalidentity.sofd.config.properties;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dk.digitalidentity.sofd.config.FeatureDocumentation;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OS2ilm {

    @FeatureDocumentation(name = "OS2ilm", description = "Synkronisering til OS2ilm")
    private boolean enabled = false;

    @JsonIgnore
    private String url;

    @JsonIgnore
    private String apiKey;
}