package com.commafeed.backend.dao;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.commafeed.backend.dao.SearchStringParser.ParsedSearch;
import com.commafeed.backend.feeds.FeedUtils;
import com.commafeed.backend.model.AbstractModel_;
import com.commafeed.backend.model.FeedEntry;
import com.commafeed.backend.model.FeedEntryContent;
import com.commafeed.backend.model.FeedEntryContent_;
import com.commafeed.backend.model.FeedEntryStatus;
import com.commafeed.backend.model.FeedEntryStatus_;
import com.commafeed.backend.model.FeedEntry_;
import com.commafeed.backend.model.FeedSubscription;
import com.commafeed.backend.model.User;
import com.commafeed.backend.model.UserSettings.ReadingOrder;
import com.commafeed.backend.services.ApplicationSettingsService;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;

@Stateless
public class FeedEntryStatusDAO extends GenericDAO<FeedEntryStatus> {

  private static final String CATEGORY_ID_SEARCH_OPTION = "categoryId";
  private static final String FEED_ID_SEARCH_OPTION = "feedId";

  protected static Logger log = LoggerFactory.getLogger(FeedEntryStatusDAO.class);

  @Inject
  private ApplicationSettingsService applicationSettingsService;

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

  private boolean searchIncludesSubscription(ParsedSearch search, FeedSubscription sub) {
    final SetMultimap<String, String> options = search.options;
    if (options.containsKey(CATEGORY_ID_SEARCH_OPTION)
        && (sub.getCategory() == null || !options.containsEntry(CATEGORY_ID_SEARCH_OPTION, sub
            .getCategory().getId().toString()))) {
      return false;
    }
    if (options.containsKey(FEED_ID_SEARCH_OPTION)
        && !options.containsEntry(FEED_ID_SEARCH_OPTION, sub.getFeed().getId().toString())) {
      return false;
    }
    return true;
  }

  public List<FeedEntryStatus> findBySubscriptions(List<FeedSubscription> subscriptions,
      String keywords, Date newerThan, int offset, int limit, ReadingOrder order,
      boolean includeContent) {

    final ParsedSearch search = SearchStringParser.parse(keywords);

    final Map<Long, FeedSubscription> filteredFeeds =
        subscriptions.stream().filter(sub -> searchIncludesSubscription(search, sub))
            .collect(Collectors.toMap(sub -> sub.getFeed().getId(), sub -> sub));

    final CriteriaQuery<FeedEntry> query = builder.createQuery(FeedEntry.class);
    final Root<FeedEntry> root = query.from(FeedEntry.class);

    final List<Predicate> predicates = Lists.newArrayList();
    predicates.add(root.get(FeedEntry_.feed).get(AbstractModel_.id).in(filteredFeeds.keySet()));

    if (newerThan != null) {
      predicates.add(builder.greaterThanOrEqualTo(root.get(FeedEntry_.inserted), newerThan));
    }

    if (!search.terms.isEmpty()) {
      final Join<FeedEntry, FeedEntryContent> contentJoin = root.join(FeedEntry_.content);

      final Set<String> searchWords = new HashSet<>();

      search.terms.forEach(term -> {
        FeedUtils.extractSearchWords(term, false, searchWords);

        final String likeTerm = "%" + term.toLowerCase() + "%";
        final Predicate content =
            builder.like(builder.lower(contentJoin.get(FeedEntryContent_.content)), likeTerm);
        final Predicate title =
            builder.like(builder.lower(contentJoin.get(FeedEntryContent_.title)), likeTerm);
        predicates.add(builder.or(content, title));
      });

      // add full text search match in addition to like, because it is indexed
      final StringBuilder ftsQueryBuilder = new StringBuilder();
      for (final String word : searchWords) {
        if (ftsQueryBuilder.length() > 0) {
          ftsQueryBuilder.append(" & ");
        }
        ftsQueryBuilder.append(word).append(":*");
      }
      predicates.add(builder.isTrue(builder.function("pg_fts_simple_match", Boolean.class,
          contentJoin.get(FeedEntryContent_.searchText),
          builder.literal(ftsQueryBuilder.toString()))));
    }

    query.where(predicates.toArray(new Predicate[0]));
    orderBy(query, root.get(FeedEntry_.updated), order, root.get(AbstractModel_.id));

    final TypedQuery<FeedEntry> q = em.createQuery(query);
    limit(q, offset, limit);
    setTimeout(q);

    final List<FeedEntry> entries = q.getResultList();
    entries.forEach(entry -> {
      entry.setSubscription(filteredFeeds.get(entry.getFeed().getId()));
    });

    final List<FeedEntryStatus> results =
        entries.stream().map(entry -> getStatus(entry.getSubscription(), entry))
            .collect(Collectors.toList());

    return lazyLoadContent(includeContent, results);
  }

  public List<FeedEntryStatus> findUnreadBySubscriptions(List<FeedSubscription> subscriptions,
      Date newerThan, int offset, int limit, ReadingOrder order, boolean includeContent) {

    final CriteriaQuery<FeedEntryStatus> query = builder.createQuery(getType());
    final Root<FeedEntryStatus> root = query.from(getType());

    final List<Predicate> predicates = Lists.newArrayList();

    predicates.add(root.get(FeedEntryStatus_.subscription).in(subscriptions));
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
  public Map<Long, Long> getUnreadCount(User user) {
    final Map<Long, Long> map = Maps.newHashMap();
    final TypedQuery<Object[]> query =
        em.createQuery("select s.subscription.id, count(s) from FeedEntryStatus s "
            + "where s.user=:user and s.read=false group by s.subscription.id", Object[].class);
    query.setParameter("user", user);
    setTimeout(query);
    final List<Object[]> resultList = query.getResultList();
    for (final Object[] row : resultList) {
      map.put((Long) row[0], (Long) row[1]);
    }
    return map;
  }

  private List<FeedEntryStatus> lazyLoadContent(boolean includeContent,
      List<FeedEntryStatus> results) {
    if (includeContent) {
      for (final FeedEntryStatus status : results) {
        Hibernate.initialize(status.getSubscription().getFeed());
        Hibernate.initialize(status.getEntry().getContent());
      }
    }
    return results;
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
