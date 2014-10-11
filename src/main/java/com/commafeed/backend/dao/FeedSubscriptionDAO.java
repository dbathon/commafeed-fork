package com.commafeed.backend.dao;

import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.hibernate.Hibernate;

import com.commafeed.backend.model.AbstractModel_;
import com.commafeed.backend.model.Feed;
import com.commafeed.backend.model.FeedCategory;
import com.commafeed.backend.model.FeedSubscription;
import com.commafeed.backend.model.FeedSubscription_;
import com.commafeed.backend.model.User;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

@Stateless
public class FeedSubscriptionDAO extends GenericDAO<FeedSubscription> {

  public FeedSubscription findById(User user, Long id) {
    final CriteriaQuery<FeedSubscription> query = builder.createQuery(getType());
    final Root<FeedSubscription> root = query.from(getType());

    final Predicate p1 =
        builder.equal(root.get(FeedSubscription_.user).get(AbstractModel_.id), user.getId());
    final Predicate p2 = builder.equal(root.get(AbstractModel_.id), id);

    root.fetch(FeedSubscription_.feed, JoinType.LEFT);
    root.fetch(FeedSubscription_.category, JoinType.LEFT);

    query.where(p1, p2);

    final FeedSubscription sub =
        Iterables.getFirst(cache(em.createQuery(query)).getResultList(), null);
    initRelations(sub);
    return sub;
  }

  public List<FeedSubscription> findByFeed(Feed feed) {
    final CriteriaQuery<FeedSubscription> query = builder.createQuery(getType());
    final Root<FeedSubscription> root = query.from(getType());

    query
        .where(builder.equal(root.get(FeedSubscription_.feed).get(AbstractModel_.id), feed.getId()));
    final List<FeedSubscription> list = cache(em.createQuery(query)).getResultList();
    initRelations(list);
    return list;
  }

  public FeedSubscription findByFeed(User user, Feed feed) {

    final CriteriaQuery<FeedSubscription> query = builder.createQuery(getType());
    final Root<FeedSubscription> root = query.from(getType());

    final Predicate p1 =
        builder.equal(root.get(FeedSubscription_.user).get(AbstractModel_.id), user.getId());
    final Predicate p2 =
        builder.equal(root.get(FeedSubscription_.feed).get(AbstractModel_.id), feed.getId());

    root.fetch(FeedSubscription_.feed, JoinType.LEFT);
    root.fetch(FeedSubscription_.category, JoinType.LEFT);

    query.where(p1, p2);

    final FeedSubscription sub =
        Iterables.getFirst(cache(em.createQuery(query)).getResultList(), null);
    initRelations(sub);
    return sub;
  }

  public List<FeedSubscription> findAll(User user) {

    final CriteriaQuery<FeedSubscription> query = builder.createQuery(getType());
    final Root<FeedSubscription> root = query.from(getType());

    root.fetch(FeedSubscription_.feed, JoinType.LEFT);
    root.fetch(FeedSubscription_.category, JoinType.LEFT);

    query
        .where(builder.equal(root.get(FeedSubscription_.user).get(AbstractModel_.id), user.getId()));

    final List<FeedSubscription> list = cache(em.createQuery(query)).getResultList();
    initRelations(list);
    return list;
  }

  public List<FeedSubscription> findByCategory(User user, FeedCategory category) {

    final CriteriaQuery<FeedSubscription> query = builder.createQuery(getType());
    final Root<FeedSubscription> root = query.from(getType());

    final Predicate p1 =
        builder.equal(root.get(FeedSubscription_.user).get(AbstractModel_.id), user.getId());
    Predicate p2 = null;
    if (category == null) {
      p2 = builder.isNull(root.get(FeedSubscription_.category));
    }
    else {
      p2 =
          builder.equal(root.get(FeedSubscription_.category).get(AbstractModel_.id),
              category.getId());

    }

    query.where(p1, p2);

    final List<FeedSubscription> list = cache(em.createQuery(query)).getResultList();
    initRelations(list);
    return list;
  }

  public List<FeedSubscription> findByCategories(User user, List<FeedCategory> categories) {

    final List<Long> categoryIds = Lists.transform(categories, input -> input.getId());

    final List<FeedSubscription> subscriptions = Lists.newArrayList();
    for (final FeedSubscription sub : findAll(user)) {
      if (sub.getCategory() != null && categoryIds.contains(sub.getCategory().getId())) {
        subscriptions.add(sub);
      }
    }
    return subscriptions;
  }

  private void initRelations(List<FeedSubscription> list) {
    for (final FeedSubscription sub : list) {
      initRelations(sub);
    }
  }

  private void initRelations(FeedSubscription sub) {
    if (sub != null) {
      Hibernate.initialize(sub.getFeed());
      Hibernate.initialize(sub.getCategory());
    }
  }
}
