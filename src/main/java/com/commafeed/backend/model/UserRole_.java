package com.commafeed.backend.model;

import com.commafeed.backend.model.UserRole.Role;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(UserRole.class)
public abstract class UserRole_ extends AbstractModel_ {

  public static volatile SingularAttribute<UserRole, Role> role;
  public static volatile SingularAttribute<UserRole, User> user;

}

