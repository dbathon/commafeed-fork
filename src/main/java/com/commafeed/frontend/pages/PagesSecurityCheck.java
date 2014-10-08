package com.commafeed.frontend.pages;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.commafeed.backend.model.UserRole.Role;

@Inherited
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface PagesSecurityCheck {

  /**
   * Roles needed.
   */
  Role value() default Role.USER;

}
