package dk.digitalidentity.sofd.security;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.springframework.security.access.prepost.PreAuthorize;

@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasRole('ROLE_USER_PERSON_CREATER') or hasRole('ROLE_USER_EDIT')")
public @interface RequirePersonCreaterOrControllerWriteAccess {

}
