package com.commafeed.backend.services;

import com.commafeed.backend.dao.FeedDAO;
import com.commafeed.backend.feeds.FeedUtils;
import com.commafeed.backend.model.Feed;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.inject.Inject;
import org.apache.commons.codec.digest.DigestUtils;

@Singleton
public class FeedService {

  @Inject
  private FeedDAO feedDAO;

  @Lock(LockType.WRITE)
  public Feed findOrCreate(String url) {
    Feed feed = feedDAO.findByUrl(url);
    if (feed == null) {
      final String normalized = FeedUtils.normalizeURL(url);
      feed = new Feed();
      feed.setUrl(url);
      feed.setUrlHash(DigestUtils.sha1Hex(url));
      feed.setNormalizedUrl(normalized);
      feed.setNormalizedUrlHash(DigestUtils.sha1Hex(normalized));
      feedDAO.saveOrUpdate(feed);
    }
    return feed;
  }

}
