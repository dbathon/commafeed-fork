package com.commafeed.frontend.pages.components;

import com.commafeed.backend.services.ApplicationSettingsService;
import javax.inject.Inject;
import org.apache.wicket.authroles.authentication.panel.SignInPanel;
import org.apache.wicket.feedback.ContainerFeedbackMessageFilter;

@SuppressWarnings("serial")
public class LoginPanel extends SignInPanel {

  @Inject
  private ApplicationSettingsService applicationSettingsService;

  public LoginPanel(String id) {
    super(id);
    replace(new BootstrapFeedbackPanel("feedback", new ContainerFeedbackMessageFilter(this)));
  }

}
