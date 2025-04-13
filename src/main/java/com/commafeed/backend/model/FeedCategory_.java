package com.commafeed.backend.model;

import javax.persistence.metamodel.SetAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(FeedCategory.class)
public abstract class FeedCategory_ extends AbstractModel_ {

	public static volatile SingularAttribute<FeedCategory, FeedCategory> parent;
	public static volatile SetAttribute<FeedCategory, FeedSubscription> subscriptions;
	public static volatile SetAttribute<FeedCategory, FeedCategory> children;
	public static volatile SingularAttribute<FeedCategory, Boolean> collapsed;
	public static volatile SingularAttribute<FeedCategory, String> name;
	public static volatile SingularAttribute<FeedCategory, Integer> position;
	public static volatile SingularAttribute<FeedCategory, User> user;

}

