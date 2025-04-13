package com.commafeed.backend.feeds;

import com.commafeed.backend.HttpGetter.NotModifiedException;
import com.commafeed.backend.feeds.FeedRefreshExecutor.Task;
import com.commafeed.backend.model.ApplicationSettings;
import com.commafeed.backend.model.Feed;
import com.commafeed.backend.model.FeedEntry;
import com.commafeed.backend.services.ApplicationSettingsService;
import com.sun.syndication.io.FeedException;
import java.util.Date;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class FeedRefreshWorker {

  private static Logger log = LoggerFactory.getLogger(FeedRefreshWorker.class);

  @Inject
  private FeedRefreshUpdater feedRefreshUpdater;

  @Inject
  private FeedFetcher fetcher;

  @Inject
  private FeedRefreshTaskGiver taskGiver;

  @Inject
  private ApplicationSettingsService applicationSettingsService;

  private FeedRefreshExecutor pool;

  @PostConstruct
  private void init() {
    final ApplicationSettings settings = applicationSettingsService.get();
    final int threads = settings.getBackgroundThreads();
    pool = new FeedRefreshExecutor("feed-refresh-worker", threads, 20 * threads);
  }

  @PreDestroy
  public void shutdown() {
    pool.shutdown();
  }

  public void updateFeed(Feed feed) {
    pool.execute(new FeedTask(feed));
  }

  public int getQueueSize() {
    return pool.getQueueSize();
  }

  public int getActiveCount() {
    return pool.getActiveCount();
  }

  private class FeedTask implements Task {

    private final Feed feed;

    public FeedTask(Feed feed) {
      this.feed = feed;
    }

    @Override
    public void run() {
      update(feed);
    }

    @Override
    public boolean isUrgent() {
      return feed.isUrgent();
    }
  }

  private void update(Feed feed) {
    final Date now = new Date();
    try {
      final FetchedFeed fetchedFeed =
          fetcher.fetch(feed.getUrl(), false, feed.getLastModifiedHeader(), feed.getEtagHeader(),
              feed.getLastPublishedDate(), feed.getLastContentHash());
      // stops here if NotModifiedException or any other exception is
      // thrown
      final List<FeedEntry> entries = fetchedFeed.getEntries();

      feed.setLastUpdateSuccess(now);
      feed.setLink(fetchedFeed.getFeed().getLink());
      feed.setLastModifiedHeader(fetchedFeed.getFeed().getLastModifiedHeader());
      feed.setEtagHeader(fetchedFeed.getFeed().getEtagHeader());
      feed.setLastContentHash(fetchedFeed.getFeed().getLastContentHash());
      feed.setLastPublishedDate(fetchedFeed.getFeed().getLastPublishedDate());
      feed.setLastEntryDate(fetchedFeed.getFeed().getLastEntryDate());

      feed.setErrorCount(0);
      feed.setMessage(null);
      feed.setDisabledUntil(null);

      feedRefreshUpdater.updateFeed(feed, entries);
    } catch (final NotModifiedException e) {
      log.debug("Feed not modified : {} - {}", feed.getUrl(), e.getMessage());

      feed.setErrorCount(0);
      feed.setMessage(null);
      feed.setDisabledUntil(null);

      taskGiver.giveBack(feed);
    } catch (final Exception e) {
      final String message = "Unable to refresh feed " + feed.getUrl() + " : " + e.getMessage();
      if (e instanceof FeedException) {
        log.debug(e.getClass().getName() + " " + message, e);
      } else {
        log.debug(e.getClass().getName() + " " + message, e);
      }

      feed.setErrorCount(feed.getErrorCount() + 1);
      feed.setMessage(message);
      feed.setDisabledUntil(FeedUtils.buildDisabledUntil(feed.getErrorCount()));

      taskGiver.giveBack(feed);
    }
  }

}
