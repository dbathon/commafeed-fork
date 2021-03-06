package com.commafeed.backend.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name = "APPLICATIONSETTINGS")
@SuppressWarnings("serial")
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ApplicationSettings extends AbstractModel {

  private String publicUrl;
  private boolean allowRegistrations = false;
  private String googleAnalyticsTrackingCode;
  private int backgroundThreads = 3;
  private int databaseUpdateThreads = 1;
  private String smtpHost;
  private int smtpPort;
  private boolean smtpTls;
  private String smtpUserName;
  private String smtpPassword;
  private boolean feedbackButton = true;
  private boolean imageProxyEnabled;
  private int queryTimeout;
  private boolean crawlingPaused;

  @Column(length = 255)
  private String announcement;

  public String getPublicUrl() {
    return publicUrl;
  }

  public void setPublicUrl(String publicUrl) {
    this.publicUrl = publicUrl;
  }

  public boolean isAllowRegistrations() {
    return allowRegistrations;
  }

  public void setAllowRegistrations(boolean allowRegistrations) {
    this.allowRegistrations = allowRegistrations;
  }

  public int getBackgroundThreads() {
    return backgroundThreads;
  }

  public void setBackgroundThreads(int backgroundThreads) {
    this.backgroundThreads = backgroundThreads;
  }

  public String getSmtpHost() {
    return smtpHost;
  }

  public void setSmtpHost(String smtpHost) {
    this.smtpHost = smtpHost;
  }

  public int getSmtpPort() {
    return smtpPort;
  }

  public void setSmtpPort(int smtpPort) {
    this.smtpPort = smtpPort;
  }

  public boolean isSmtpTls() {
    return smtpTls;
  }

  public void setSmtpTls(boolean smtpTls) {
    this.smtpTls = smtpTls;
  }

  public String getSmtpUserName() {
    return smtpUserName;
  }

  public void setSmtpUserName(String smtpUserName) {
    this.smtpUserName = smtpUserName;
  }

  public String getSmtpPassword() {
    return smtpPassword;
  }

  public void setSmtpPassword(String smtpPassword) {
    this.smtpPassword = smtpPassword;
  }

  public String getGoogleAnalyticsTrackingCode() {
    return googleAnalyticsTrackingCode;
  }

  public void setGoogleAnalyticsTrackingCode(String googleAnalyticsTrackingCode) {
    this.googleAnalyticsTrackingCode = googleAnalyticsTrackingCode;
  }

  public String getAnnouncement() {
    return announcement;
  }

  public void setAnnouncement(String announcement) {
    this.announcement = announcement;
  }

  public boolean isFeedbackButton() {
    return feedbackButton;
  }

  public void setFeedbackButton(boolean feedbackButton) {
    this.feedbackButton = feedbackButton;
  }

  public int getDatabaseUpdateThreads() {
    return databaseUpdateThreads;
  }

  public void setDatabaseUpdateThreads(int databaseUpdateThreads) {
    this.databaseUpdateThreads = databaseUpdateThreads;
  }

  public boolean isImageProxyEnabled() {
    return imageProxyEnabled;
  }

  public void setImageProxyEnabled(boolean imageProxyEnabled) {
    this.imageProxyEnabled = imageProxyEnabled;
  }

  public int getQueryTimeout() {
    return queryTimeout;
  }

  public void setQueryTimeout(int queryTimeout) {
    this.queryTimeout = queryTimeout;
  }

  public boolean isCrawlingPaused() {
    return crawlingPaused;
  }

  public void setCrawlingPaused(boolean crawlingPaused) {
    this.crawlingPaused = crawlingPaused;
  }

}
