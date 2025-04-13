package com.commafeed.frontend.pages;

import javax.inject.Inject;

import org.apache.wicket.markup.html.link.BookmarkablePageLink;

import com.commafeed.backend.services.ApplicationSettingsService;
import com.commafeed.frontend.pages.components.LoginPanel;

@SuppressWarnings("serial")
public class WelcomePage extends BasePage {

  @Inject
  private ApplicationSettingsService applicationSettingsService;

  public WelcomePage() {
    add(new BookmarkablePageLink<Void>("logo-link", getApplication().getHomePage()));
    add(new LoginPanel("login"));
  }
}
