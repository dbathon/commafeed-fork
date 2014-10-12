package com.commafeed.backend.dao;

import java.util.Date;
import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;

import com.commafeed.backend.feeds.FeedUtils;
import com.commafeed.backend.model.Feed;
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

}
