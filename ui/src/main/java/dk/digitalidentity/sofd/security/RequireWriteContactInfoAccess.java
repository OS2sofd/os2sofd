package dk.digitalidentity.sofd.security;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.springframework.security.access.prepost.PreAuthorize;

@Retention(RetentionPolicy.RUNTIME)
@RequireRoleAnnotation
@PreAuthorize("hasRole('ROLE_USER_EDIT_CONTACT_INFO')")
public @interface RequireWriteContactInfoAccess {

}
