package com.commafeed.frontend.rest.resources;

import com.commafeed.backend.dao.UserDAO;
import com.commafeed.backend.model.User;
import com.commafeed.backend.model.UserRole.Role;
import com.commafeed.backend.services.ApplicationSettingsService;
import com.commafeed.frontend.CommaFeedApplication;
import com.commafeed.frontend.CommaFeedSession;
import com.commafeed.frontend.rest.RestSecurityCheck;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import org.apache.wicket.ThreadContext;
import org.apache.wicket.authentication.IAuthenticationStrategy;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.protocol.http.servlet.ServletWebResponse;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.util.crypt.Base64;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RestSecurityCheck(Role.USER)
public abstract class AbstractREST {

  @Inject
  protected ApplicationSettingsService applicationSettingsService;

  @Context
  private HttpServletRequest request;

  @Context
  private HttpServletResponse response;

  @Inject
  private UserDAO userDAO;

  @PostConstruct
  public void init() {
    final CommaFeedApplication app = CommaFeedApplication.get();
    final ServletWebRequest swreq = new ServletWebRequest(request, "");
    final ServletWebResponse swresp = new ServletWebResponse(swreq, response);
    final RequestCycle cycle = app.createRequestCycle(swreq, swresp);
    ThreadContext.setRequestCycle(cycle);
    final CommaFeedSession session = (CommaFeedSession) app.fetchCreateAndSetSession(cycle);

    if (session.getUser() == null) {
      cookieLogin(app, session);
    }
    if (session.getUser() == null) {
      basicHttpLogin(swreq, session);
    }
  }

  private void cookieLogin(CommaFeedApplication app, CommaFeedSession session) {
    final IAuthenticationStrategy authenticationStrategy =
        app.getSecuritySettings().getAuthenticationStrategy();
    final String[] data = authenticationStrategy.load();
    if (data != null && data.length > 1) {
      session.signIn(data[0], data[1]);
    }
  }

  private void basicHttpLogin(ServletWebRequest req, CommaFeedSession session) {
    String value = req.getHeader(HttpHeaders.AUTHORIZATION);
    if (value != null && value.startsWith("Basic ")) {
      value = value.substring(6);
      final String decoded = new String(Base64.decodeBase64(value));
      final String[] data = decoded.split(":");
      if (data != null && data.length > 1) {
        session.signIn(data[0], data[1]);
      }
    }
  }

  public void apiKeyLogin() {
    final String apiKey = request.getParameter("apiKey");
    final User user = userDAO.findByApiKey(apiKey);
    CommaFeedSession.get().setUser(user);
  }

  protected User getUser() {
    return CommaFeedSession.get().getUser();
  }

}
