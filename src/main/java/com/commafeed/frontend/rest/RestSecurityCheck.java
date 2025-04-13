package com.commafeed.frontend.rest;

import com.commafeed.backend.model.UserRole.Role;
import java.lang.annotation.*;
import javax.enterprise.util.Nonbinding;
import javax.interceptor.InterceptorBinding;

@Inherited
@InterceptorBinding
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RestSecurityCheck {

  /**
   * Roles needed.
   */
  @Nonbinding
  Role value() default Role.USER;

  @Nonbinding
  boolean apiKeyAllowed() default false;
}
