package com.commafeed.backend.model;

import javax.persistence.metamodel.SetAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(FeedSubscription.class)
public abstract class FeedSubscription_ extends AbstractModel_ {

  public static volatile SingularAttribute<FeedSubscription, Feed> feed;
  public static volatile SetAttribute<FeedSubscription, FeedEntryStatus> statuses;
  public static volatile SingularAttribute<FeedSubscription, Integer> position;
  public static volatile SingularAttribute<FeedSubscription, String> title;
  public static volatile SingularAttribute<FeedSubscription, FeedCategory> category;
  public static volatile SingularAttribute<FeedSubscription, User> user;

}

