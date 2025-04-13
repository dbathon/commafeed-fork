package com.commafeed.backend.services;

import java.util.ResourceBundle;

public class ApplicationPropertiesService {

  private final ResourceBundle bundle;

  private static ApplicationPropertiesService INSTANCE = new ApplicationPropertiesService();

  public static ApplicationPropertiesService get() {
    return INSTANCE;
  }

  private ApplicationPropertiesService() {
    bundle = ResourceBundle.getBundle("application");
  }

  public String getDatasource() {
    return bundle.getString("datasource");
  }

  public String getVersion() {
    return bundle.getString("version");
  }

  public boolean isProduction() {
    return Boolean.valueOf(bundle.getString("production"));
  }
}
