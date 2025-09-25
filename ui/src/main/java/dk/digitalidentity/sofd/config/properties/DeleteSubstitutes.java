package dk.digitalidentity.sofd.config.properties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeleteSubstitutes {
    private boolean enabled = false;
    private int days = 7;
}
