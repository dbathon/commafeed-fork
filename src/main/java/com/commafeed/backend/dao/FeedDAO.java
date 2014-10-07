package com.commafeed.backend.dao;

import java.util.Date;
import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.SetJoin;
import javax.persistence.metamodel.SingularAttribute;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;

import com.commafeed.backend.feeds.FeedUtils;
import com.commafeed.backend.model.AbstractModel_;
import com.commafeed.backend.model.Feed;
import com.commafeed.backend.model.FeedSubscription;
import com.commafeed.backend.model.Feed_;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

@Stateless
public class FeedDAO extends GenericDAO<Feed> {

  @XmlRootElement
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class FeedCount {
    public String value;
    public List<Feed> feeds;
  }

  private List<Predicate> getUpdatablePredicates(Root<Feed> root, Date threshold) {

    final Predicate hasSubscriptions = builder.isNotEmpty(root.get(Feed_.subscriptions));

    final Predicate neverUpdated = builder.isNull(root.get(Feed_.lastUpdated));
    final Predicate updatedBeforeThreshold =
        builder.lessThan(root.get(Feed_.lastUpdated), threshold);

    final Predicate disabledDateIsNull = builder.isNull(root.get(Feed_.disabledUntil));
    final Predicate disabledDateIsInPast =
        builder.lessThan(root.get(Feed_.disabledUntil), new Date());

    return Lists.newArrayList(hasSubscriptions, builder.or(neverUpdated, updatedBeforeThreshold),
        builder.or(disabledDateIsNull, disabledDateIsInPast));
  }

  public Long getUpdatableCount(Date threshold) {
    final CriteriaQuery<Long> query = builder.createQuery(Long.class);
    final Root<Feed> root = query.from(getType());

    query.select(builder.count(root));
    query.where(getUpdatablePredicates(root, threshold).toArray(new Predicate[0]));

    final TypedQuery<Long> q = em.createQuery(query);
    return q.getSingleResult();
  }

  public List<Feed> findNextUpdatable(int count, Date threshold) {
    final CriteriaQuery<Feed> query = builder.createQuery(getType());
    final Root<Feed> root = query.from(getType());

    query.where(getUpdatablePredicates(root, threshold).toArray(new Predicate[0]));

    query.orderBy(builder.asc(root.get(Feed_.lastUpdated)));

    final TypedQuery<Feed> q = em.createQuery(query);
    q.setMaxResults(count);

    return q.getResultList();
  }

  public Feed findByUrl(String url) {
    List<Feed> feeds = findByField(Feed_.urlHash, DigestUtils.sha1Hex(url));
    Feed feed = Iterables.getFirst(feeds, null);
    if (feed != null && StringUtils.equals(url, feed.getUrl())) {
      return feed;
    }

    final String normalized = FeedUtils.normalizeURL(url);
    feeds = findByField(Feed_.normalizedUrlHash, DigestUtils.sha1Hex(normalized));
    feed = Iterables.getFirst(feeds, null);
    if (feed != null && StringUtils.equals(normalized, feed.getNormalizedUrl())) {
      return feed;
    }

    return null;
  }

  public List<Feed> findByTopic(String topic) {
    return findByField(Feed_.pushTopicHash, DigestUtils.sha1Hex(topic));
  }

  public void deleteRelationships(Feed feed) {
    final Query relationshipDeleteQuery = em.createNamedQuery("Feed.deleteEntryRelationships");
    relationshipDeleteQuery.setParameter("feedId", feed.getId());
    relationshipDeleteQuery.executeUpdate();
  }

  public int deleteWithoutSubscriptions(int max) {
    final CriteriaQuery<Feed> query = builder.createQuery(getType());
    final Root<Feed> root = query.from(getType());

    final SetJoin<Feed, FeedSubscription> join = root.join(Feed_.subscriptions, JoinType.LEFT);
    query.where(builder.isNull(join.get(AbstractModel_.id)));
    final TypedQuery<Feed> q = em.createQuery(query);
    q.setMaxResults(max);

    final List<Feed> list = q.getResultList();
    final int deleted = list.size();

    for (final Feed feed : list) {
      deleteRelationships(feed);
      delete(feed);
    }
    return deleted;

  }

  public static enum DuplicateMode {
    NORMALIZED_URL(Feed_.normalizedUrlHash),
    LAST_CONTENT(Feed_.lastContentHash);

    private final SingularAttribute<Feed, String> path;

    private DuplicateMode(SingularAttribute<Feed, String> path) {
      this.path = path;
    }

    public SingularAttribute<Feed, String> getPath() {
      return path;
    }
  }

  public List<FeedCount> findDuplicates(DuplicateMode mode, int offset, int limit, long minCount) {
    final CriteriaQuery<String> query = builder.createQuery(String.class);
    final Root<Feed> root = query.from(getType());

    final Path<String> path = root.get(mode.getPath());
    final Expression<Long> count = builder.count(path);

    query.select(path);

    query.groupBy(path);
    query.having(builder.greaterThan(count, minCount));

    final TypedQuery<String> q = em.createQuery(query);
    limit(q, offset, limit);
    final List<String> pathValues = q.getResultList();

    final List<FeedCount> result = Lists.newArrayList();
    for (final String pathValue : pathValues) {
      final FeedCount fc = new FeedCount();
      fc.value = pathValue;
      fc.feeds = Lists.newArrayList();
      for (final Feed feed : findByField(mode.getPath(), pathValue)) {
        final Feed f = new Feed();
        f.setId(feed.getId());
        f.setUrl(feed.getUrl());
        fc.feeds.add(f);
      }
      result.add(fc);
    }
    return result;
  }
}
