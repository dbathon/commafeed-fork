package com.commafeed.backend.services;

import com.commafeed.backend.MetricsBean;
import com.commafeed.backend.dao.FeedEntryDAO;
import com.commafeed.backend.dao.FeedEntryStatusDAO;
import com.commafeed.backend.model.*;
import com.google.common.collect.Lists;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import javax.ejb.*;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Stateless
public class FeedUpdateService {

  private static Logger log = LoggerFactory.getLogger(FeedUpdateService.class);

  @PersistenceContext
  private EntityManager em;

  @EJB
  private FeedUpdateService self;

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

      content.updateSearchText();
    }
  }

  public void updateEntry(Feed feed, FeedEntry entry, List<FeedSubscription> subscriptions) {
    final FeedEntry existing =
        feedEntryDAO.findExisting(entry.getGuid(), entry.getUrl(), feed.getId());

    if (existing == null) {
      entry.setInserted(new Date());
      entry.setFeed(feed);
      entry.getContent().updateSearchText();
      feedEntryDAO.saveOrUpdate(entry);

      createAndSaveEntryStatuses(entry, subscriptions);
    } else {
      // just update the content if there are changes
      processContentChanges(existing, entry.getContent());
    }
  }

  @Asynchronous
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public void updateFeedEntryContentSearchTextsAsync() {
    Long lastId = Long.MIN_VALUE;
    // do the update in batches
    while (lastId != null) {
      lastId = self.internalUpdateFeedEntryContentSearch(lastId);
    }
  }

  public Long internalUpdateFeedEntryContentSearch(Long lastId) {
    Long resultLastId = null;

    final TypedQuery<FeedEntryContent> query =
        em.createQuery("select c from FeedEntryContent c "
            + "where c.searchText is null and c.id > :lastId order by c.id", FeedEntryContent.class);
    query.setParameter("lastId", lastId);
    query.setMaxResults(100);
    final List<FeedEntryContent> items = query.getResultList();

    for (final FeedEntryContent item : items) {
      item.updateSearchText();
      resultLastId = item.getId();
    }

    log.info("updated search text of " + items.size() + " entry contents");
    return resultLastId;
  }

}
