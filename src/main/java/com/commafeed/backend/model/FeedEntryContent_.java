package com.commafeed.backend.model;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(FeedEntryContent.class)
public abstract class FeedEntryContent_ extends AbstractModel_ {

	public static volatile SingularAttribute<FeedEntryContent, String> enclosureUrl;
	public static volatile SingularAttribute<FeedEntryContent, String> searchText;
	public static volatile SingularAttribute<FeedEntryContent, String> enclosureType;
	public static volatile SingularAttribute<FeedEntryContent, String> title;
	public static volatile SingularAttribute<FeedEntryContent, String> content;

}

