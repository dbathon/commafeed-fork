package com.commafeed.backend.services;

import java.util.Date;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.commafeed.backend.MetricsBean;
import com.commafeed.backend.dao.FeedEntryDAO;
import com.commafeed.backend.dao.FeedEntryDAO.EntryWithFeed;
import com.commafeed.backend.dao.FeedEntryStatusDAO;
import com.commafeed.backend.feeds.FeedUtils;
import com.commafeed.backend.model.Feed;
import com.commafeed.backend.model.FeedEntry;
import com.commafeed.backend.model.FeedEntryContent;
import com.commafeed.backend.model.FeedEntryStatus;
import com.commafeed.backend.model.FeedFeedEntry;
import com.commafeed.backend.model.FeedSubscription;
import com.commafeed.backend.model.User;
import com.google.common.collect.Lists;

@Stateless
public class FeedUpdateService {

  @PersistenceContext
  protected EntityManager em;

  @Inject
  private FeedEntryDAO feedEntryDAO;

  @Inject
  private FeedEntryStatusDAO feedEntryStatusDAO;

  @Inject
  private MetricsBean metricsBean;

  public void updateEntry(Feed feed, FeedEntry entry, List<FeedSubscription> subscriptions) {

    final EntryWithFeed existing =
        feedEntryDAO.findExisting(entry.getGuid(), entry.getUrl(), feed.getId());

    FeedEntry update = null;
    FeedFeedEntry ffe = null;
    if (existing == null) {
      entry.setAuthor(FeedUtils.truncate(
          FeedUtils.handleContent(entry.getAuthor(), feed.getLink(), true), 128));
      final FeedEntryContent content = entry.getContent();
      content.setTitle(FeedUtils.truncate(
          FeedUtils.handleContent(content.getTitle(), feed.getLink(), true), 2048));
      content.setContent(FeedUtils.handleContent(content.getContent(), feed.getLink(), false));

      entry.setInserted(new Date());
      ffe = new FeedFeedEntry(feed, entry);

      update = entry;
    }
    else if (existing.ffe == null) {
      ffe = new FeedFeedEntry(feed, existing.entry);
      update = existing.entry;
    }

    if (update != null) {
      final List<FeedEntryStatus> statusUpdateList = Lists.newArrayList();
      final List<User> users = Lists.newArrayList();
      for (final FeedSubscription sub : subscriptions) {
        final User user = sub.getUser();
        final FeedEntryStatus status = new FeedEntryStatus(user, sub, update);
        status.setSubscription(sub);
        statusUpdateList.add(status);
        users.add(user);
      }
      feedEntryDAO.saveOrUpdate(update);
      feedEntryStatusDAO.saveOrUpdate(statusUpdateList);
      em.persist(ffe);
      metricsBean.entryUpdated(statusUpdateList.size());
    }
  }
}
