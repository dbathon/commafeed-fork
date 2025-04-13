package com.commafeed.backend.model;

import java.util.Set;
import java.util.TreeSet;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.commafeed.backend.feeds.FeedUtils;
import com.google.common.base.Joiner;

@Entity
@Table(name = "FEEDENTRYCONTENTS")
@SuppressWarnings("serial")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
public class FeedEntryContent extends AbstractModel {

  private static final Joiner SPACE_JOINER = Joiner.on(" ");

  @Column(length = 2048)
  private String title;

  @Column(length = Integer.MAX_VALUE, columnDefinition = "text")
  private String content;

  /**
   * The "words" extracted from title and content for full text search.
   */
  @Column(length = Integer.MAX_VALUE, columnDefinition = "text")
  private String searchText;

  @Column(length = 2048)
  private String enclosureUrl;

  @Column(length = 255)
  private String enclosureType;

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getEnclosureUrl() {
    return enclosureUrl;
  }

  public void setEnclosureUrl(String enclosureUrl) {
    this.enclosureUrl = enclosureUrl;
  }

  public String getEnclosureType() {
    return enclosureType;
  }

  public void setEnclosureType(String enclosureType) {
    this.enclosureType = enclosureType;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getSearchText() {
    return searchText;
  }

  public void setSearchText(String searchText) {
    this.searchText = searchText;
  }

  public void updateSearchText() {
    final Set<String> searchWords = new TreeSet<>();
    FeedUtils.extractSearchWords(getTitle(), false, searchWords);
    FeedUtils.extractSearchWords(getContent(), true, searchWords);

    setSearchText(SPACE_JOINER.join(searchWords));
  }

}
