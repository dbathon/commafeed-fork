package com.commafeed.backend.services;

import java.util.Date;
import java.util.List;
import java.util.Objects;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.commafeed.backend.MetricsBean;
import com.commafeed.backend.dao.FeedEntryDAO;
import com.commafeed.backend.dao.FeedEntryDAO.EntryWithFeed;
import com.commafeed.backend.dao.FeedEntryStatusDAO;
import com.commafeed.backend.model.Feed;
import com.commafeed.backend.model.FeedEntry;
import com.commafeed.backend.model.FeedEntryContent;
import com.commafeed.backend.model.FeedEntryStatus;
import com.commafeed.backend.model.FeedFeedEntry;
import com.commafeed.backend.model.FeedSubscription;
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

  private void createAndSaveEntryStatuses(FeedEntry feedEntry, List<FeedSubscription> subscriptions) {
    final List<FeedEntryStatus> statusUpdateList = Lists.newArrayList();
    for (final FeedSubscription sub : subscriptions) {
      statusUpdateList.add(new FeedEntryStatus(sub.getUser(), sub, feedEntry));
    }
    feedEntryStatusDAO.saveOrUpdate(statusUpdateList);

    metricsBean.entryUpdated(statusUpdateList.size());
  }

  private void processContentChanges(FeedEntry entry, FeedEntryContent newContent) {
    final FeedEntryContent oldContent = entry.getContent();
    final boolean different =
        !Objects.equals(oldContent.getTitle(), newContent.getTitle())
            || !Objects.equals(oldContent.getEnclosureType(), newContent.getEnclosureType())
            || !Objects.equals(oldContent.getEnclosureUrl(), newContent.getEnclosureUrl())
            || !Objects.equals(oldContent.getContent(), newContent.getContent());
    if (different) {
      if (entry.getOriginalContent() == null) {
        // first change, keep the original content
        entry.setOriginalContent(entry.getContent());
        entry.setContent(new FeedEntryContent());
      }

      final FeedEntryContent content = entry.getContent();
      content.setTitle(newContent.getTitle());
      content.setContent(newContent.getContent());
      content.setEnclosureType(newContent.getEnclosureType());
      content.setEnclosureUrl(newContent.getEnclosureUrl());
    }
  }

  public void updateEntry(Feed feed, FeedEntry entry, List<FeedSubscription> subscriptions) {
    final EntryWithFeed existing =
        feedEntryDAO.findExisting(entry.getGuid(), entry.getUrl(), feed.getId());

    if (existing == null) {
      entry.setInserted(new Date());
      feedEntryDAO.saveOrUpdate(entry);

      createAndSaveEntryStatuses(entry, subscriptions);
      em.persist(new FeedFeedEntry(feed, entry));
    }
    else if (existing.ffe == null) {
      createAndSaveEntryStatuses(existing.entry, subscriptions);
      em.persist(new FeedFeedEntry(feed, existing.entry));

      processContentChanges(existing.entry, entry.getContent());
    }
    else {
      // just update the content if there are changes
      processContentChanges(existing.entry, entry.getContent());
    }
  }

}
