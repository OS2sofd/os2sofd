package dk.digitalidentity.sofd.security;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.springframework.security.access.prepost.PreAuthorize;

@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasRole('ROLE_WRITE_ACCESS') or hasRole('ROLE_USER_EDIT') or hasRole('ROLE_USER_EDIT_CONTACT_INFO')")
public @interface RequireDaoWriteAccess {

}
