package com.commafeed.backend.dao;

import com.commafeed.backend.model.AbstractModel_;
import com.commafeed.backend.model.User;
import com.commafeed.backend.model.UserRole;
import com.commafeed.backend.model.UserRole.Role;
import com.commafeed.backend.model.UserRole_;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import javax.ejb.Stateless;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;

@Stateless
public class UserRoleDAO extends GenericDAO<UserRole> {

  @Override
  public List<UserRole> findAll() {
    final CriteriaQuery<UserRole> query = builder.createQuery(getType());
    final Root<UserRole> root = query.from(getType());
    query.distinct(true);

    root.fetch(UserRole_.user, JoinType.LEFT);

    return em.createQuery(query).getResultList();
  }

  public List<UserRole> findAll(User user) {
    final CriteriaQuery<UserRole> query = builder.createQuery(getType());
    final Root<UserRole> root = query.from(getType());

    query.where(builder.equal(root.get(UserRole_.user).get(AbstractModel_.id), user.getId()));
    return cache(em.createQuery(query)).getResultList();
  }

  public Set<Role> findRoles(User user) {
    final Set<Role> list = Sets.newHashSet();
    for (final UserRole role : findAll(user)) {
      list.add(role.getRole());
    }
    return list;
  }
}
