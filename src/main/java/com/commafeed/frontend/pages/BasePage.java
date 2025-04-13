package com.commafeed.frontend.pages;

import java.util.Map;

import javax.inject.Inject;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.filter.HeaderResponseContainer;
import org.apache.wicket.markup.html.TransparentWebMarkupContainer;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;

import com.commafeed.backend.StartupBean;
import com.commafeed.backend.dao.FeedCategoryDAO;
import com.commafeed.backend.dao.FeedDAO;
import com.commafeed.backend.dao.FeedEntryDAO;
import com.commafeed.backend.dao.FeedEntryStatusDAO;
import com.commafeed.backend.dao.FeedSubscriptionDAO;
import com.commafeed.backend.dao.UserDAO;
import com.commafeed.backend.dao.UserRoleDAO;
import com.commafeed.backend.dao.UserSettingsDAO;
import com.commafeed.backend.model.ApplicationSettings;
import com.commafeed.backend.model.User;
import com.commafeed.backend.model.UserSettings;
import com.commafeed.backend.services.ApplicationSettingsService;
import com.commafeed.frontend.CommaFeedSession;

@SuppressWarnings("serial")
public abstract class BasePage extends WebPage {

  @Inject
  protected FeedDAO feedDAO;

  @Inject
  private StartupBean startupBean;

  @Inject
  protected FeedSubscriptionDAO feedSubscriptionDAO;

  @Inject
  protected FeedCategoryDAO feedCategoryDAO;

  @Inject
  protected FeedEntryDAO feedEntryDAO;

  @Inject
  protected FeedEntryStatusDAO feedEntryStatusDAO;

  @Inject
  protected UserDAO userDAO;

  @Inject
  protected UserSettingsDAO userSettingsDAO;

  @Inject
  protected UserRoleDAO userRoleDAO;

  @Inject
  protected ApplicationSettingsService applicationSettingsService;

  private final ApplicationSettings settings;

  public BasePage() {

    String lang = "en";
    String theme = "default";
    final User user = CommaFeedSession.get().getUser();
    if (user != null) {
      final UserSettings settings = userSettingsDAO.findByUser(user);
      if (settings != null) {
        lang = settings.getLanguage() == null ? "en" : settings.getLanguage();
        theme = settings.getTheme() == null ? "default" : settings.getTheme();
      }
    }

    add(new TransparentWebMarkupContainer("html").setMarkupId("theme-" + theme)
        .add(new AttributeModifier("lang", lang))
        .add(new AttributeModifier("timestamp", Long.toString(startupBean.getStartupTime()))));

    settings = applicationSettingsService.get();
    add(new HeaderResponseContainer("footer-container", "footer-container"));
  }

  @Override
  public void renderHead(IHeaderResponse response) {
    super.renderHead(response);

    final boolean production =
        getApplication().getConfigurationType() == RuntimeConfigurationType.DEPLOYMENT;
    final String suffix = production ? "?" + startupBean.getStartupTime() : "";
    response.render(JavaScriptHeaderItem.forUrl("static/all.js" + suffix));
    response.render(CssHeaderItem.forUrl("static/all.css" + suffix));
  }
}
