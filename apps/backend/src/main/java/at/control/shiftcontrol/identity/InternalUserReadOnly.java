package at.control.shiftcontrol.identity;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.access.prepost.PreAuthorize;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize(Authorities.HAS_PLATFORM_ADMIN + " or hasAuthority('" + Authorities.INTERNAL_USERS_READ + "')")
public @interface InternalUserReadOnly {
}
