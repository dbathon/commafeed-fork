package com.commafeed.backend.services;

import com.commafeed.backend.dao.ApplicationSettingsDAO;
import com.commafeed.backend.model.ApplicationSettings;
import com.google.common.collect.Iterables;
import javax.ejb.Singleton;
import javax.inject.Inject;

@Singleton
public class ApplicationSettingsService {

  @Inject
  private ApplicationSettingsDAO applicationSettingsDAO;

  private ApplicationSettings settings;

  public void save(ApplicationSettings settings) {
    this.settings = settings;
    applicationSettingsDAO.saveOrUpdate(settings);
  }

  public ApplicationSettings get() {
    if (settings == null) {
      settings = Iterables.getFirst(applicationSettingsDAO.findAll(), null);
    }
    return settings;
  }

}
