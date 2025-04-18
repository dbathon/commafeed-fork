package com.commafeed.frontend;

import com.commafeed.frontend.pages.HomePage;
import com.commafeed.frontend.pages.LogoutPage;
import com.commafeed.frontend.pages.PagesSecurityCheck;
import com.commafeed.frontend.pages.WelcomePage;
import java.util.ResourceBundle;
import javax.enterprise.inject.spi.BeanManager;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.Cookie;
import org.apache.wicket.*;
import org.apache.wicket.authentication.strategy.DefaultAuthenticationStrategy;
import org.apache.wicket.authorization.Action;
import org.apache.wicket.authorization.IAuthorizationStrategy;
import org.apache.wicket.authroles.authentication.AbstractAuthenticatedWebSession;
import org.apache.wicket.authroles.authentication.AuthenticatedWebApplication;
import org.apache.wicket.authroles.authorization.strategies.role.Roles;
import org.apache.wicket.cdi.CdiConfiguration;
import org.apache.wicket.cdi.ConversationPropagation;
import org.apache.wicket.markup.head.filter.JavaScriptFilteredIntoFooterHeaderResponse;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.component.IRequestableComponent;
import org.apache.wicket.util.cookies.CookieUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommaFeedApplication extends AuthenticatedWebApplication {

  private static Logger log = LoggerFactory.getLogger(CommaFeedApplication.class);

  public CommaFeedApplication() {
    super();
    final String prod = ResourceBundle.getBundle("application").getString("production");
    setConfigurationType(Boolean.valueOf(prod) ? RuntimeConfigurationType.DEPLOYMENT
        : RuntimeConfigurationType.DEVELOPMENT);
  }

  @Override
  protected void init() {
    super.init();

    mountPage("welcome", WelcomePage.class);

    mountPage("logout", LogoutPage.class);

    setupInjection();
    setupSecurity();

    getMarkupSettings().setStripWicketTags(true);
    getMarkupSettings().setCompressWhitespace(true);
    getMarkupSettings().setDefaultMarkupEncoding("UTF-8");

    setHeaderResponseDecorator(response -> new JavaScriptFilteredIntoFooterHeaderResponse(response,
        "footer-container"));
  }

  private void setupSecurity() {
    getSecuritySettings().setAuthenticationStrategy(new DefaultAuthenticationStrategy("LoggedIn") {

      private CookieUtils cookieUtils = null;

      @Override
      protected CookieUtils getCookieUtils() {

        if (cookieUtils == null) {
          cookieUtils = new CookieUtils() {
            @Override
            protected void initializeCookie(Cookie cookie) {
              super.initializeCookie(cookie);
              cookie.setHttpOnly(true);
            }
          };
        }
        return cookieUtils;
      }
    });
    getSecuritySettings().setAuthorizationStrategy(new IAuthorizationStrategy() {

      @Override
      public <T extends IRequestableComponent> boolean isInstantiationAuthorized(
          Class<T> componentClass) {
        boolean authorized = true;

        final boolean restricted = componentClass.isAnnotationPresent(PagesSecurityCheck.class);
        if (restricted) {
          final PagesSecurityCheck annotation =
              componentClass.getAnnotation(PagesSecurityCheck.class);
          final Roles roles = CommaFeedSession.get().getRoles();
          authorized = roles.hasAnyRole(new Roles(annotation.value().name()));
        }
        return authorized;
      }

      @Override
      public boolean isActionAuthorized(Component component, Action action) {
        return true;
      }
    });
  }

  @Override
  public Class<? extends Page> getHomePage() {
    return HomePage.class;
  }

  protected void setupInjection() {
    try {
      final BeanManager beanManager =
          (BeanManager) new InitialContext().lookup("java:comp/BeanManager");
      new CdiConfiguration(beanManager).setPropagation(ConversationPropagation.NONE)
          .configure(this);
    } catch (final NamingException e) {
      log.warn("Could not locate bean manager. CDI is disabled.");
    }
  }

  @Override
  public Session newSession(Request request, Response response) {
    return new CommaFeedSession(request);
  }

  @Override
  protected Class<? extends WebPage> getSignInPageClass() {
    return WelcomePage.class;
  }

  @Override
  protected Class<? extends AbstractAuthenticatedWebSession> getWebSessionClass() {
    return CommaFeedSession.class;
  }

  public static CommaFeedApplication get() {

    return (CommaFeedApplication) Application.get();
  }
}
