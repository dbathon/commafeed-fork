package com.commafeed.frontend.pages;

import org.apache.wicket.markup.html.WebPage;

import com.commafeed.backend.StartupBean;
import com.commafeed.frontend.CommaFeedSession;

public class DemoLoginPage extends WebPage {

  private static final long serialVersionUID = 1L;

  public DemoLoginPage() {
    CommaFeedSession.get().authenticate(StartupBean.USERNAME_DEMO, StartupBean.USERNAME_DEMO);
    setResponsePage(getApplication().getHomePage());
  }
}
