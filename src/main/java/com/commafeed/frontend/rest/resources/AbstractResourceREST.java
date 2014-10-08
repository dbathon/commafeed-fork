package com.commafeed.frontend.rest.resources;

import javax.inject.Inject;

import com.commafeed.backend.model.UserRole.Role;
import com.commafeed.backend.services.ApplicationSettingsService;
import com.commafeed.frontend.rest.RestSecurityCheck;

@RestSecurityCheck(Role.USER)
public abstract class AbstractResourceREST extends AbstractREST {

  @Inject
  protected ApplicationSettingsService applicationSettingsService;

}
