package dk.digitalidentity.sofd.security;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasRole('ROLE_USER_ADMIN') or hasRole('ROLE_USER_CHART_EDITOR')")
public @interface RequireAdminOrChartEditorAccess {

}
