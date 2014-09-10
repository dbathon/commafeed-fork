package com.commafeed.backend.dao;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.commafeed.backend.FixedSizeSortedSet;
import com.commafeed.backend.model.AbstractModel;
import com.commafeed.backend.model.AbstractModel_;
import com.commafeed.backend.model.FeedEntry;
import com.commafeed.backend.model.FeedEntryContent;
import com.commafeed.backend.model.FeedEntryContent_;
import com.commafeed.backend.model.FeedEntryStatus;
import com.commafeed.backend.model.FeedEntryStatus_;
import com.commafeed.backend.model.FeedEntry_;
import com.commafeed.backend.model.FeedFeedEntry;
import com.commafeed.backend.model.FeedFeedEntry_;
import com.commafeed.backend.model.FeedSubscription;
import com.commafeed.backend.model.Models;
import com.commafeed.backend.model.User;
import com.commafeed.backend.model.UserSettings.ReadingOrder;
import com.commafeed.backend.services.ApplicationSettingsService;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@Stateless
public class FeedEntryStatusDAO extends GenericDAO<FeedEntryStatus> {

  protected static Logger log = LoggerFactory.getLogger(FeedEntryStatusDAO.class);

  private static int compareDatesAndIds(Date date1, Date date2, AbstractModel e1, AbstractModel e2) {
    final int c1 = ObjectUtils.compare(date1, date2);
    if (c1 == 0) {
      return ObjectUtils.compare(e1.getId(), e2.getId());
    }
    else {
      return c1;
    }
  }

  private static final Comparator<FeedEntry> ENTRY_COMPARATOR_DESC =
      (o1, o2) -> compareDatesAndIds(o2.getUpdated(), o1.getUpdated(), o2, o1);

  private static final Comparator<FeedEntry> ENTRY_COMPARATOR_ASC = (o1, o2) -> compareDatesAndIds(
      o1.getUpdated(), o2.getUpdated(), o1, o2);

  private static final Comparator<FeedEntryStatus> STATUS_COMPARATOR_DESC =
      (o1, o2) -> compareDatesAndIds(o2.getEntryUpdated(), o1.getEntryUpdated(), o2, o1);

  private static final Comparator<FeedEntryStatus> STATUS_COMPARATOR_ASC =
      (o1, o2) -> compareDatesAndIds(o1.getEntryUpdated(), o2.getEntryUpdated(), o1, o2);

  @Inject
  ApplicationSettingsService applicationSettingsService;

  public FeedEntryStatus getStatus(FeedSubscription sub, FeedEntry entry) {

    final CriteriaQuery<FeedEntryStatus> query = builder.createQuery(getType());
    final Root<FeedEntryStatus> root = query.from(getType());

    final Predicate p1 = builder.equal(root.get(FeedEntryStatus_.entry), entry);
    final Predicate p2 = builder.equal(root.get(FeedEntryStatus_.subscription), sub);

    query.where(p1, p2);

    final List<FeedEntryStatus> statuses = em.createQuery(query).getResultList();
    FeedEntryStatus status = Iterables.getFirst(statuses, null);
    if (status == null) {
      status = new FeedEntryStatus(sub.getUser(), sub, entry);
      status.setRead(true);
    }
    return status;
  }

  public List<FeedEntryStatus> findStarred(User user, Date newerThan, int offset, int limit,
      ReadingOrder order, boolean includeContent) {

    final CriteriaQuery<FeedEntryStatus> query = builder.createQuery(getType());
    final Root<FeedEntryStatus> root = query.from(getType());

    final List<Predicate> predicates = Lists.newArrayList();

    predicates.add(builder.equal(root.get(FeedEntryStatus_.user), user));
    predicates.add(builder.equal(root.get(FeedEntryStatus_.starred), true));
    query.where(predicates.toArray(new Predicate[0]));

    if (newerThan != null) {
      predicates.add(builder.greaterThanOrEqualTo(root.get(FeedEntryStatus_.entryInserted),
          newerThan));
    }

    orderStatusesBy(query, root, order, root.get(AbstractModel_.id));

    final TypedQuery<FeedEntryStatus> q = em.createQuery(query);
    limit(q, offset, limit);
    setTimeout(q);
    return lazyLoadContent(includeContent, q.getResultList());
  }

  public List<FeedEntryStatus> findBySubscriptions(List<FeedSubscription> subscriptions,
      String keywords, Date newerThan, int offset, int limit, ReadingOrder order,
      boolean includeContent) {

    final int capacity = offset + limit;
    final Comparator<FeedEntry> comparator =
        order == ReadingOrder.desc ? ENTRY_COMPARATOR_DESC : ENTRY_COMPARATOR_ASC;
    final FixedSizeSortedSet<FeedEntry> set =
        new FixedSizeSortedSet<FeedEntry>(capacity < 0 ? Integer.MAX_VALUE : capacity, comparator);
    for (final FeedSubscription sub : subscriptions) {
      final CriteriaQuery<FeedEntry> query = builder.createQuery(FeedEntry.class);
      final Root<FeedEntry> root = query.from(FeedEntry.class);
      final Join<FeedEntry, FeedFeedEntry> ffeJoin = root.join(FeedEntry_.feedRelationships);

      final List<Predicate> predicates = Lists.newArrayList();
      predicates.add(builder.equal(ffeJoin.get(FeedFeedEntry_.feed), sub.getFeed()));

      if (newerThan != null) {
        predicates.add(builder.greaterThanOrEqualTo(root.get(FeedEntry_.inserted), newerThan));
      }

      if (keywords != null) {
        final Join<FeedEntry, FeedEntryContent> contentJoin = root.join(FeedEntry_.content);

        String joinedKeywords = StringUtils.join(keywords.toLowerCase().split(" "), "%");
        joinedKeywords = "%" + joinedKeywords + "%";

        final Predicate content =
            builder.like(builder.lower(contentJoin.get(FeedEntryContent_.content)), joinedKeywords);
        final Predicate title =
            builder.like(builder.lower(contentJoin.get(FeedEntryContent_.title)), joinedKeywords);
        predicates.add(builder.or(content, title));
      }

      if (order != null && !set.isEmpty() && set.isFull()) {
        Predicate filter = null;
        final FeedEntry last = set.last();
        if (order == ReadingOrder.desc) {
          filter = builder.greaterThan(ffeJoin.get(FeedFeedEntry_.entryUpdated), last.getUpdated());
        }
        else {
          filter = builder.lessThan(ffeJoin.get(FeedFeedEntry_.entryUpdated), last.getUpdated());
        }
        predicates.add(filter);
      }
      query.where(predicates.toArray(new Predicate[0]));
      orderEntriesBy(query, ffeJoin, order, root.get(AbstractModel_.id));
      final TypedQuery<FeedEntry> q = em.createQuery(query);
      limit(q, 0, capacity);
      setTimeout(q);

      final List<FeedEntry> list = q.getResultList();
      for (final FeedEntry entry : list) {
        entry.setSubscription(sub);
      }
      set.addAll(list);
    }

    List<FeedEntry> entries = set.asList();
    final int size = entries.size();
    if (size < offset) {
      return Lists.newArrayList();
    }

    entries = entries.subList(Math.max(offset, 0), size);

    final List<FeedEntryStatus> results = Lists.newArrayList();
    for (final FeedEntry entry : entries) {
      final FeedSubscription subscription = entry.getSubscription();
      results.add(getStatus(subscription, entry));
    }

    return lazyLoadContent(includeContent, results);
  }

  public List<FeedEntryStatus> findUnreadBySubscriptions(List<FeedSubscription> subscriptions,
      Date newerThan, int offset, int limit, ReadingOrder order, boolean includeContent) {

    final int capacity = offset + limit;
    final Comparator<FeedEntryStatus> comparator =
        order == ReadingOrder.desc ? STATUS_COMPARATOR_DESC : STATUS_COMPARATOR_ASC;
    final FixedSizeSortedSet<FeedEntryStatus> set =
        new FixedSizeSortedSet<FeedEntryStatus>(capacity < 0 ? Integer.MAX_VALUE : capacity,
            comparator);
    for (final FeedSubscription sub : subscriptions) {
      final CriteriaQuery<FeedEntryStatus> query = builder.createQuery(getType());
      final Root<FeedEntryStatus> root = query.from(getType());

      final List<Predicate> predicates = Lists.newArrayList();

      predicates.add(builder.equal(root.get(FeedEntryStatus_.subscription), sub));
      predicates.add(builder.isFalse(root.get(FeedEntryStatus_.read)));

      if (newerThan != null) {
        predicates.add(builder.greaterThanOrEqualTo(root.get(FeedEntryStatus_.entryInserted),
            newerThan));
      }

      if (order != null && !set.isEmpty() && set.isFull()) {
        Predicate filter = null;
        final FeedEntryStatus last = set.last();
        if (order == ReadingOrder.desc) {
          filter =
              builder.greaterThan(root.get(FeedEntryStatus_.entryUpdated), last.getEntryUpdated());
        }
        else {
          filter =
              builder.lessThan(root.get(FeedEntryStatus_.entryUpdated), last.getEntryUpdated());
        }
        predicates.add(filter);
      }
      query.where(predicates.toArray(new Predicate[0]));
      orderStatusesBy(query, root, order, root.get(AbstractModel_.id));

      final TypedQuery<FeedEntryStatus> q = em.createQuery(query);
      limit(q, 0, capacity);
      setTimeout(q);

      final List<FeedEntryStatus> list = q.getResultList();
      set.addAll(list);
    }

    List<FeedEntryStatus> entries = set.asList();
    final int size = entries.size();
    if (size < offset) {
      return Lists.newArrayList();
    }

    entries = entries.subList(Math.max(offset, 0), size);
    return lazyLoadContent(includeContent, entries);
  }

  public List<FeedEntryStatus> findAllUnread(User user, Date newerThan, int offset, int limit,
      ReadingOrder order, boolean includeContent) {

    final CriteriaQuery<FeedEntryStatus> query = builder.createQuery(getType());
    final Root<FeedEntryStatus> root = query.from(getType());

    final List<Predicate> predicates = Lists.newArrayList();

    predicates.add(builder.equal(root.get(FeedEntryStatus_.user), user));
    predicates.add(builder.isFalse(root.get(FeedEntryStatus_.read)));

    if (newerThan != null) {
      predicates.add(builder.greaterThanOrEqualTo(root.get(FeedEntryStatus_.entryInserted),
          newerThan));
    }

    query.where(predicates.toArray(new Predicate[0]));
    orderStatusesBy(query, root, order, root.get(AbstractModel_.id));

    final TypedQuery<FeedEntryStatus> q = em.createQuery(query);
    limit(q, offset, limit);
    setTimeout(q);

    return lazyLoadContent(includeContent, q.getResultList());
  }

  /**
   * Map between subscriptionId and unread count
   */
  @SuppressWarnings("rawtypes")
  public Map<Long, Long> getUnreadCount(User user) {
    final Map<Long, Long> map = Maps.newHashMap();
    final Query query = em.createNamedQuery("EntryStatus.unreadCounts");
    query.setParameter("user", user);
    setTimeout(query);
    final List resultList = query.getResultList();
    for (final Object o : resultList) {
      final Object[] array = (Object[]) o;
      map.put((Long) array[0], (Long) array[1]);
    }
    return map;
  }

  private List<FeedEntryStatus> lazyLoadContent(boolean includeContent,
      List<FeedEntryStatus> results) {
    if (includeContent) {
      for (final FeedEntryStatus status : results) {
        Models.initialize(status.getSubscription().getFeed());
        Models.initialize(status.getEntry().getContent());
      }
    }
    return results;
  }

  private void orderEntriesBy(CriteriaQuery<?> query, Path<FeedFeedEntry> ffeJoin,
      ReadingOrder order, Path<Long> id) {
    orderBy(query, ffeJoin.get(FeedFeedEntry_.entryUpdated), order, id);
  }

  private void orderStatusesBy(CriteriaQuery<?> query, Path<FeedEntryStatus> statusJoin,
      ReadingOrder order, Path<Long> id) {
    orderBy(query, statusJoin.get(FeedEntryStatus_.entryUpdated), order, id);
  }

  private void orderBy(CriteriaQuery<?> query, Path<Date> date, ReadingOrder order, Path<Long> id) {
    if (order != null) {
      if (order == ReadingOrder.asc) {
        query.orderBy(builder.asc(date), builder.asc(id));
      }
      else {
        query.orderBy(builder.desc(date), builder.desc(id));
      }
    }
  }

  protected void setTimeout(Query query) {
    setTimeout(query, applicationSettingsService.get().getQueryTimeout());
  }

  public void markAllEntries(User user, Date olderThan) {
    final List<FeedEntryStatus> statuses = findAllUnread(user, null, -1, -1, null, false);
    markList(statuses, olderThan);
  }

  public void markSubscriptionEntries(List<FeedSubscription> subscriptions, Date olderThan) {
    final List<FeedEntryStatus> statuses =
        findUnreadBySubscriptions(subscriptions, null, -1, -1, null, false);
    markList(statuses, olderThan);
  }

  public void markStarredEntries(User user, Date olderThan) {
    final List<FeedEntryStatus> statuses = findStarred(user, null, -1, -1, null, false);
    markList(statuses, olderThan);
  }

  private void markList(List<FeedEntryStatus> statuses, Date olderThan) {
    final List<FeedEntryStatus> list = Lists.newArrayList();
    for (final FeedEntryStatus status : statuses) {
      if (!status.isRead()) {
        final Date inserted = status.getEntry().getInserted();
        if (olderThan == null || inserted == null || olderThan.after(inserted)) {
          if (status.isStarred()) {
            status.setRead(true);
            list.add(status);
          }
          else {
            delete(status);
          }

        }
      }
    }
    saveOrUpdate(list);
  }

}
