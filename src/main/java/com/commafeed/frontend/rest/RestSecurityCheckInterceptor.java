package com.commafeed.frontend.rest;

import java.io.Serializable;
import java.lang.reflect.Method;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.wicket.authroles.authorization.strategies.role.Roles;

import com.commafeed.backend.model.User;
import com.commafeed.backend.model.UserRole.Role;
import com.commafeed.frontend.CommaFeedSession;
import com.commafeed.frontend.rest.resources.AbstractREST;

@RestSecurityCheck
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class RestSecurityCheckInterceptor implements Serializable {

  private User getUser() {
    return CommaFeedSession.get().getUser();
  }

  @AroundInvoke
  public Object aroundInvoke(InvocationContext context) throws Exception {
    final Method method = context.getMethod();
    RestSecurityCheck check = method.getAnnotation(RestSecurityCheck.class);
    if (check == null) {
      check = method.getDeclaringClass().getAnnotation(RestSecurityCheck.class);
    }

    User user = null;
    boolean allowed = false;
    if (check != null) {
      user = getUser();
      if (user == null && check.apiKeyAllowed()) {
        final Object target = context.getTarget();
        if (target instanceof AbstractREST) {
          ((AbstractREST) target).apiKeyLogin();
          user = getUser();
        }
      }

      allowed = checkRole(check.value());
    }

    if (!allowed) {
      if (user == null) {
        return Response.status(Status.UNAUTHORIZED).entity("You are not authorized to do this.")
            .build();
      }
      else {
        return Response.status(Status.FORBIDDEN).entity("You are not authorized to do this.")
            .build();
      }
    }

    return context.proceed();
  }

  private boolean checkRole(Role requiredRole) {
    if (requiredRole == Role.NONE) {
      return true;
    }

    final Roles roles = CommaFeedSession.get().getRoles();
    return roles.hasAnyRole(new Roles(requiredRole.name()));
  }

}
