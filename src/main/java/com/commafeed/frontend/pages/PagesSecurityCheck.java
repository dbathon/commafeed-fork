package com.commafeed.frontend.pages;

import com.commafeed.backend.model.UserRole.Role;
import java.lang.annotation.*;

@Inherited
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface PagesSecurityCheck {

  /**
   * Roles needed.
   */
  Role value() default Role.USER;

}
