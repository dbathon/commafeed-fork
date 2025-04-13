package com.commafeed.backend.dao;

import com.commafeed.backend.model.AbstractModel_;
import com.commafeed.backend.model.FeedCategory;
import com.commafeed.backend.model.FeedCategory_;
import com.commafeed.backend.model.User;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.apache.commons.lang.ObjectUtils;

@Stateless
public class FeedCategoryDAO extends GenericDAO<FeedCategory> {

  @SuppressWarnings("unchecked")
  public List<FeedCategory> findAll(User user) {

    final CriteriaQuery<FeedCategory> query = builder.createQuery(getType());
    final Root<FeedCategory> root = query.from(getType());
    final Join<FeedCategory, User> userJoin =
        (Join<FeedCategory, User>) root.fetch(FeedCategory_.user);

    query.where(builder.equal(userJoin.get(AbstractModel_.id), user.getId()));

    return cache(em.createQuery(query)).getResultList();
  }

  public FeedCategory findById(User user, Long id) {
    final CriteriaQuery<FeedCategory> query = builder.createQuery(getType());
    final Root<FeedCategory> root = query.from(getType());

    final Predicate p1 =
        builder.equal(root.get(FeedCategory_.user).get(AbstractModel_.id), user.getId());
    final Predicate p2 = builder.equal(root.get(AbstractModel_.id), id);

    query.where(p1, p2);

    return Iterables.getFirst(cache(em.createQuery(query)).getResultList(), null);
  }

  public FeedCategory findByName(User user, String name, FeedCategory parent) {
    final CriteriaQuery<FeedCategory> query = builder.createQuery(getType());
    final Root<FeedCategory> root = query.from(getType());

    final List<Predicate> predicates = Lists.newArrayList();

    predicates.add(builder.equal(root.get(FeedCategory_.user), user));
    predicates.add(builder.equal(root.get(FeedCategory_.name), name));

    if (parent == null) {
      predicates.add(builder.isNull(root.get(FeedCategory_.parent)));
    } else {
      predicates.add(builder.equal(root.get(FeedCategory_.parent), parent));
    }

    query.where(predicates.toArray(new Predicate[0]));

    FeedCategory category = null;
    try {
      category = em.createQuery(query).getSingleResult();
    } catch (final NoResultException e) {
      category = null;
    }
    return category;
  }

  public List<FeedCategory> findByParent(User user, FeedCategory parent) {
    final CriteriaQuery<FeedCategory> query = builder.createQuery(getType());
    final Root<FeedCategory> root = query.from(getType());

    final List<Predicate> predicates = Lists.newArrayList();

    predicates.add(builder.equal(root.get(FeedCategory_.user), user));
    if (parent == null) {
      predicates.add(builder.isNull(root.get(FeedCategory_.parent)));
    } else {
      predicates.add(builder.equal(root.get(FeedCategory_.parent), parent));
    }

    query.where(predicates.toArray(new Predicate[0]));

    return em.createQuery(query).getResultList();
  }

  public List<FeedCategory> findAllChildrenCategories(User user, FeedCategory parent) {
    final List<FeedCategory> list = Lists.newArrayList();
    final List<FeedCategory> all = findAll(user);
    for (final FeedCategory cat : all) {
      if (isChild(cat, parent)) {
        list.add(cat);
      }
    }
    return list;
  }

  public boolean isChild(FeedCategory child, FeedCategory parent) {
    if (parent == null) {
      return true;
    }
    boolean isChild = false;
    while (child != null) {
      if (ObjectUtils.equals(child.getId(), parent.getId())) {
        isChild = true;
        break;
      }
      child = child.getParent();
    }
    return isChild;
  }

}
