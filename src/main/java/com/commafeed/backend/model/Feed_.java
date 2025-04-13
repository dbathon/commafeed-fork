package com.commafeed.backend.model;

import javax.persistence.metamodel.SetAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.util.Date;

@StaticMetamodel(Feed.class)
public abstract class Feed_ extends AbstractModel_ {

	public static volatile SingularAttribute<Feed, String> normalizedUrl;
	public static volatile SetAttribute<Feed, FeedSubscription> subscriptions;
	public static volatile SingularAttribute<Feed, Date> lastPublishedDate;
	public static volatile SingularAttribute<Feed, String> urlHash;
	public static volatile SingularAttribute<Feed, String> link;
	public static volatile SingularAttribute<Feed, Date> disabledUntil;
	public static volatile SingularAttribute<Feed, String> lastModifiedHeader;
	public static volatile SingularAttribute<Feed, String> etagHeader;
	public static volatile SingularAttribute<Feed, Date> lastUpdateSuccess;
	public static volatile SingularAttribute<Feed, String> message;
	public static volatile SingularAttribute<Feed, String> url;
	public static volatile SingularAttribute<Feed, String> normalizedUrlHash;
	public static volatile SingularAttribute<Feed, Date> lastUpdated;
	public static volatile SingularAttribute<Feed, String> lastContentHash;
	public static volatile SingularAttribute<Feed, Date> lastEntryDate;
	public static volatile SingularAttribute<Feed, Integer> errorCount;

}

