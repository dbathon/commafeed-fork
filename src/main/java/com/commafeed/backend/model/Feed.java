package com.commafeed.backend.model;

import java.util.Date;
import java.util.Set;
import javax.persistence.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name = "FEEDS", indexes = {@Index(columnList = "disabledUntil, lastUpdated"),
    @Index(columnList = "lastUpdated"), @Index(columnList = "urlHash"),
    @Index(columnList = "normalizedUrlHash"), @Index(columnList = "lastContentHash")})
@SuppressWarnings("serial")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
public class Feed extends AbstractModel {

  /**
   * The url of the feed
   */
  @Column(length = 2048, nullable = false)
  private String url;

  @Column(length = 40, nullable = false)
  private String urlHash;

  @Column(length = 2048)
  private String normalizedUrl;

  @Column(length = 40)
  private String normalizedUrlHash;

  /**
   * The url of the website, extracted from the feed
   */
  @Column(length = 2048)
  private String link;

  /**
   * Last time we tried to fetch the feed
   */
  @Temporal(TemporalType.TIMESTAMP)
  private Date lastUpdated;

  /**
   * Last publishedDate value in the feed
   */
  @Temporal(TemporalType.TIMESTAMP)
  private Date lastPublishedDate;

  /**
   * date of the last entry of the feed
   */
  @Temporal(TemporalType.TIMESTAMP)
  private Date lastEntryDate;

  /**
   * Last time we successfully refreshed the feed
   */
  @Temporal(TemporalType.TIMESTAMP)
  private Date lastUpdateSuccess;

  /**
   * error message while retrieving the feed
   */
  @Column(length = 1024)
  private String message;

  /**
   * times we failed to retrieve the feed
   */
  private int errorCount;

  /**
   * feed refresh is disabled until this date
   */
  @Temporal(TemporalType.TIMESTAMP)
  private Date disabledUntil;

  /**
   * http header returned by the feed
   */
  @Column(length = 64)
  private String lastModifiedHeader;

  /**
   * http header returned by the feed
   */
  @Column(length = 255)
  private String etagHeader;

  /**
   * last hash of the content of the feed xml
   */
  @Column(length = 40)
  private String lastContentHash;

  @OneToMany(mappedBy = "feed")
  private Set<FeedSubscription> subscriptions;

  /**
   * Denotes a feed that needs to be refreshed before others. Currently used when a feed is queued
   * manually for refresh. Not persisted.
   */
  @Transient
  private boolean urgent;

  public Feed() {

  }

  public Feed(String url) {
    this.url = url;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public Date getLastUpdated() {
    return lastUpdated;
  }

  public void setLastUpdated(Date lastUpdated) {
    this.lastUpdated = lastUpdated;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public Set<FeedSubscription> getSubscriptions() {
    return subscriptions;
  }

  public void setSubscriptions(Set<FeedSubscription> subscriptions) {
    this.subscriptions = subscriptions;
  }

  public String getLink() {
    return link;
  }

  public void setLink(String link) {
    this.link = link;
  }

  public int getErrorCount() {
    return errorCount;
  }

  public void setErrorCount(int errorCount) {
    this.errorCount = errorCount;
  }

  public Date getDisabledUntil() {
    return disabledUntil;
  }

  public void setDisabledUntil(Date disabledUntil) {
    this.disabledUntil = disabledUntil;
  }

  public String getUrlHash() {
    return urlHash;
  }

  public void setUrlHash(String urlHash) {
    this.urlHash = urlHash;
  }

  public String getLastModifiedHeader() {
    return lastModifiedHeader;
  }

  public void setLastModifiedHeader(String lastModifiedHeader) {
    this.lastModifiedHeader = lastModifiedHeader;
  }

  public String getEtagHeader() {
    return etagHeader;
  }

  public void setEtagHeader(String etagHeader) {
    this.etagHeader = etagHeader;
  }

  public Date getLastUpdateSuccess() {
    return lastUpdateSuccess;
  }

  public void setLastUpdateSuccess(Date lastUpdateSuccess) {
    this.lastUpdateSuccess = lastUpdateSuccess;
  }

  public Date getLastPublishedDate() {
    return lastPublishedDate;
  }

  public void setLastPublishedDate(Date lastPublishedDate) {
    this.lastPublishedDate = lastPublishedDate;
  }

  public String getLastContentHash() {
    return lastContentHash;
  }

  public void setLastContentHash(String lastContentHash) {
    this.lastContentHash = lastContentHash;
  }

  public Date getLastEntryDate() {
    return lastEntryDate;
  }

  public void setLastEntryDate(Date lastEntryDate) {
    this.lastEntryDate = lastEntryDate;
  }

  public boolean isUrgent() {
    return urgent;
  }

  public void setUrgent(boolean urgent) {
    this.urgent = urgent;
  }

  public String getNormalizedUrl() {
    return normalizedUrl;
  }

  public void setNormalizedUrl(String normalizedUrl) {
    this.normalizedUrl = normalizedUrl;
  }

  public String getNormalizedUrlHash() {
    return normalizedUrlHash;
  }

  public void setNormalizedUrlHash(String normalizedUrlHash) {
    this.normalizedUrlHash = normalizedUrlHash;
  }

}
