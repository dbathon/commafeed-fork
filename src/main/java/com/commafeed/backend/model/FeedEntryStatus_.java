package com.commafeed.backend.model;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.util.Date;

@StaticMetamodel(FeedEntryStatus.class)
public abstract class FeedEntryStatus_ extends AbstractModel_ {

	public static volatile SingularAttribute<FeedEntryStatus, Date> entryUpdated;
	public static volatile SingularAttribute<FeedEntryStatus, FeedEntry> entry;
	public static volatile SingularAttribute<FeedEntryStatus, Boolean> read;
	public static volatile SingularAttribute<FeedEntryStatus, Date> entryInserted;
	public static volatile SingularAttribute<FeedEntryStatus, Boolean> starred;
	public static volatile SingularAttribute<FeedEntryStatus, FeedSubscription> subscription;
	public static volatile SingularAttribute<FeedEntryStatus, User> user;

}

