package com.commafeed.backend.dao;

import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.commafeed.backend.model.Feed;
import com.commafeed.backend.model.FeedEntry;
import com.commafeed.backend.model.FeedEntry_;
import com.commafeed.backend.services.ApplicationSettingsService;
import com.google.common.collect.Iterables;

@Stateless
public class FeedEntryDAO extends GenericDAO<FeedEntry> {

  @Inject
  private ApplicationSettingsService applicationSettingsService;

  protected static final Logger log = LoggerFactory.getLogger(FeedEntryDAO.class);

  public FeedEntry findExisting(String guid, String url, Long feedId) {
    final TypedQuery<FeedEntry> q =
        em.createQuery("select e FROM FeedEntry e "
            + "where e.feed.id = :feedId and e.guidHash = :guidHash and e.url = :url",
            FeedEntry.class);
    q.setParameter("guidHash", DigestUtils.sha1Hex(guid));
    q.setParameter("url", url);
    q.setParameter("feedId", feedId);

    return Iterables.getFirst(q.getResultList(), null);
  }

  public List<FeedEntry> findByFeed(Feed feed, int offset, int limit) {
    final CriteriaQuery<FeedEntry> query = builder.createQuery(getType());
    final Root<FeedEntry> root = query.from(getType());

    query.where(builder.equal(root.get(FeedEntry_.feed), feed));
    query.orderBy(builder.desc(root.get(FeedEntry_.updated)));
    final TypedQuery<FeedEntry> q = em.createQuery(query);
    limit(q, offset, limit);
    setTimeout(q, applicationSettingsService.get().getQueryTimeout());
    return q.getResultList();
  }

}
