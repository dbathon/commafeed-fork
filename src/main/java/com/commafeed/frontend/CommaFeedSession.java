package com.commafeed.frontend;

import com.commafeed.backend.dao.UserRoleDAO;
import com.commafeed.backend.model.User;
import com.commafeed.backend.services.UserService;
import javax.inject.Inject;
import org.apache.wicket.Session;
import org.apache.wicket.authroles.authentication.AuthenticatedWebSession;
import org.apache.wicket.authroles.authorization.strategies.role.Roles;
import org.apache.wicket.request.Request;

public class CommaFeedSession extends AuthenticatedWebSession {

  private static final long serialVersionUID = 1L;

  @Inject
  private UserService userService;

  @Inject
  private UserRoleDAO userRoleDAO;

  private User user;
  private Roles roles = new Roles();

  public CommaFeedSession(Request request) {
    super(request);
  }

  public User getUser() {
    return user;
  }

  public static CommaFeedSession get() {
    return (CommaFeedSession) Session.get();
  }

  @Override
  public Roles getRoles() {
    return roles;
  }

  @Override
  public boolean authenticate(String userName, String password) {
    final User user = userService.login(userName, password);
    setUser(user);
    return user != null;
  }

  public void setUser(User user) {
    if (user == null) {
      this.user = null;
      roles = new Roles();
    } else {
      this.user = user;
      final String[] rolesArray =
          userRoleDAO.findRoles(user).stream().map(role -> role.name()).toArray(String[]::new);
      roles = new Roles(rolesArray);
    }
  }
}
