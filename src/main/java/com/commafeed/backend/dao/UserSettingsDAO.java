package com.commafeed.backend.dao;

import com.commafeed.backend.model.AbstractModel_;
import com.commafeed.backend.model.User;
import com.commafeed.backend.model.UserSettings;
import com.commafeed.backend.model.UserSettings_;
import javax.ejb.Stateless;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

@Stateless
public class UserSettingsDAO extends GenericDAO<UserSettings> {

  public UserSettings findByUser(User user) {

    final CriteriaQuery<UserSettings> query = builder.createQuery(getType());
    final Root<UserSettings> root = query.from(getType());

    query.where(builder.equal(root.get(UserSettings_.user).get(AbstractModel_.id), user.getId()));

    UserSettings settings = null;
    try {
      settings = cache(em.createQuery(query)).getSingleResult();
    } catch (final NoResultException e) {
      settings = null;
    }
    return settings;
  }
}
