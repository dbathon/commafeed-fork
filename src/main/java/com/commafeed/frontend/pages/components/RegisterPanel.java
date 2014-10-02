package com.commafeed.frontend.pages.components;

import java.util.Arrays;

import javax.inject.Inject;

import org.apache.wicket.authentication.IAuthenticationStrategy;
import org.apache.wicket.feedback.ContainerFeedbackMessageFilter;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.StatelessForm;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.apache.wicket.validation.validator.StringValidator;

import com.commafeed.backend.dao.UserDAO;
import com.commafeed.backend.model.User;
import com.commafeed.backend.model.UserRole.Role;
import com.commafeed.backend.services.ApplicationSettingsService;
import com.commafeed.backend.services.UserService;
import com.commafeed.frontend.CommaFeedSession;
import com.commafeed.frontend.model.request.RegistrationRequest;
import com.commafeed.frontend.pages.RfcCompliantEmailAddressValidator;

@SuppressWarnings("serial")
public class RegisterPanel extends Panel {

  @Inject
  private UserDAO userDAO;

  @Inject
  private UserService userService;

  @Inject
  private ApplicationSettingsService applicationSettingsService;

  public RegisterPanel(String markupId) {
    super(markupId);

    final IModel<RegistrationRequest> model = Model.of(new RegistrationRequest());

    final Form<RegistrationRequest> form = new StatelessForm<RegistrationRequest>("form", model) {
      @Override
      protected void onSubmit() {
        if (applicationSettingsService.get().isAllowRegistrations()) {
          final RegistrationRequest req = getModelObject();
          userService.register(req.getName(), req.getPassword(), req.getEmail(),
              Arrays.asList(Role.USER));

          final IAuthenticationStrategy strategy =
              getApplication().getSecuritySettings().getAuthenticationStrategy();
          strategy.save(req.getName(), req.getPassword());
          CommaFeedSession.get().signIn(req.getName(), req.getPassword());
        }
        setResponsePage(getApplication().getHomePage());
      }
    };
    add(form);
    add(new BootstrapFeedbackPanel("feedback", new ContainerFeedbackMessageFilter(form)));

    form.add(new RequiredTextField<>("name", new PropertyModel<String>(model, "name")).add(
        StringValidator.lengthBetween(3, 32)).add((IValidator<String>) validatable -> {
      final String name = validatable.getValue();
      final User user = userDAO.findByName(name);
      if (user != null) {
        validatable.error(new ValidationError("Name is already taken."));
      }
    }));
    form.add(new PasswordTextField("password", new PropertyModel<String>(model, "password"))
        .setResetPassword(false).add(StringValidator.minimumLength(6)));
    form.add(new RequiredTextField<String>("email", new PropertyModel<String>(model, "email")) {
      @Override
      protected String getInputType() {
        return "email";
      }
    }.add(RfcCompliantEmailAddressValidator.getInstance()).add(
        (IValidator<String>) validatable -> {
          final String email = validatable.getValue();
          final User user = userDAO.findByEmail(email);
          if (user != null) {
            validatable.error(new ValidationError("Email is already taken."));
          }
        }));
  }
}
