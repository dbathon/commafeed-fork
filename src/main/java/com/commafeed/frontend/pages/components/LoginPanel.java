package com.commafeed.frontend.pages.components;

import javax.inject.Inject;

import org.apache.wicket.authroles.authentication.panel.SignInPanel;
import org.apache.wicket.feedback.ContainerFeedbackMessageFilter;

import com.commafeed.backend.services.ApplicationSettingsService;

@SuppressWarnings("serial")
public class LoginPanel extends SignInPanel {

  @Inject
  private ApplicationSettingsService applicationSettingsService;

  public LoginPanel(String id) {
    super(id);
    replace(new BootstrapFeedbackPanel("feedback", new ContainerFeedbackMessageFilter(this)));
  }

}
