package com.commafeed.backend.model;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(ApplicationSettings.class)
public abstract class ApplicationSettings_ extends AbstractModel_ {

  public static volatile SingularAttribute<ApplicationSettings, String> googleAnalyticsTrackingCode;
  public static volatile SingularAttribute<ApplicationSettings, Integer> queryTimeout;
  public static volatile SingularAttribute<ApplicationSettings, Integer> backgroundThreads;
  public static volatile SingularAttribute<ApplicationSettings, Boolean> smtpTls;
  public static volatile SingularAttribute<ApplicationSettings, Integer> smtpPort;
  public static volatile SingularAttribute<ApplicationSettings, Boolean> feedbackButton;
  public static volatile SingularAttribute<ApplicationSettings, String> smtpHost;
  public static volatile SingularAttribute<ApplicationSettings, String> smtpUserName;
  public static volatile SingularAttribute<ApplicationSettings, Boolean> crawlingPaused;
  public static volatile SingularAttribute<ApplicationSettings, String> publicUrl;
  public static volatile SingularAttribute<ApplicationSettings, String> smtpPassword;
  public static volatile SingularAttribute<ApplicationSettings, Boolean> imageProxyEnabled;
  public static volatile SingularAttribute<ApplicationSettings, Integer> databaseUpdateThreads;
  public static volatile SingularAttribute<ApplicationSettings, Boolean> allowRegistrations;
  public static volatile SingularAttribute<ApplicationSettings, String> announcement;

}

