package com.commafeed.backend.model;

import com.commafeed.backend.model.UserSettings.ReadingMode;
import com.commafeed.backend.model.UserSettings.ReadingOrder;
import com.commafeed.backend.model.UserSettings.ViewMode;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(UserSettings.class)
public abstract class UserSettings_ extends AbstractModel_ {

	public static volatile SingularAttribute<UserSettings, ReadingOrder> readingOrder;
	public static volatile SingularAttribute<UserSettings, ReadingMode> readingMode;
	public static volatile SingularAttribute<UserSettings, String> language;
	public static volatile SingularAttribute<UserSettings, String> theme;
	public static volatile SingularAttribute<UserSettings, ViewMode> viewMode;
	public static volatile SingularAttribute<UserSettings, User> user;
	public static volatile SingularAttribute<UserSettings, Boolean> showRead;
	public static volatile SingularAttribute<UserSettings, Boolean> scrollMarks;
	public static volatile SingularAttribute<UserSettings, Boolean> socialButtons;
	public static volatile SingularAttribute<UserSettings, String> customCss;

}

