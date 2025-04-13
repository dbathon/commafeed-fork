package com.commafeed.backend.model;

import java.util.Date;
import javax.persistence.metamodel.SetAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(FeedEntry.class)
public abstract class FeedEntry_ extends AbstractModel_ {

  public static volatile SingularAttribute<FeedEntry, Feed> feed;
  public static volatile SingularAttribute<FeedEntry, Date> inserted;
  public static volatile SingularAttribute<FeedEntry, String> guidHash;
  public static volatile SingularAttribute<FeedEntry, String> author;
  public static volatile SingularAttribute<FeedEntry, String> guid;
  public static volatile SetAttribute<FeedEntry, FeedEntryStatus> statuses;
  public static volatile SingularAttribute<FeedEntry, Date> updated;
  public static volatile SingularAttribute<FeedEntry, FeedEntryContent> content;
  public static volatile SingularAttribute<FeedEntry, FeedEntryContent> originalContent;
  public static volatile SingularAttribute<FeedEntry, String> url;

}

