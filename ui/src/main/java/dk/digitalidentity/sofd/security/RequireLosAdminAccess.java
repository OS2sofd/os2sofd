package dk.digitalidentity.sofd.security;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.springframework.security.access.prepost.PreAuthorize;

@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasRole('ROLE_USER_LOS_ADMIN')")
public @interface RequireLosAdminAccess {

}
