package com.commafeed.backend.model;

import java.util.Date;
import javax.persistence.metamodel.SetAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(User.class)
public abstract class User_ extends AbstractModel_ {

  public static volatile SingularAttribute<User, Date> lastLogin;
  public static volatile SingularAttribute<User, byte[]> password;
  public static volatile SetAttribute<User, FeedSubscription> subscriptions;
  public static volatile SingularAttribute<User, byte[]> salt;
  public static volatile SingularAttribute<User, String> recoverPasswordToken;
  public static volatile SingularAttribute<User, String> apiKey;
  public static volatile SingularAttribute<User, Date> created;
  public static volatile SetAttribute<User, UserRole> roles;
  public static volatile SingularAttribute<User, String> name;
  public static volatile SingularAttribute<User, Date> recoverPasswordTokenDate;
  public static volatile SingularAttribute<User, Boolean> disabled;
  public static volatile SingularAttribute<User, String> email;

}

