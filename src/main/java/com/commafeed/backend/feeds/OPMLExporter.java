package com.commafeed.backend.feeds;

import com.commafeed.backend.dao.FeedCategoryDAO;
import com.commafeed.backend.dao.FeedSubscriptionDAO;
import com.commafeed.backend.model.FeedCategory;
import com.commafeed.backend.model.FeedSubscription;
import com.commafeed.backend.model.User;
import com.sun.syndication.feed.opml.Attribute;
import com.sun.syndication.feed.opml.Opml;
import com.sun.syndication.feed.opml.Outline;
import java.util.Date;
import java.util.List;
import javax.ejb.Stateless;
import javax.inject.Inject;

@Stateless
public class OPMLExporter {

  @Inject
  private FeedCategoryDAO feedCategoryDAO;

  @Inject
  private FeedSubscriptionDAO feedSubscriptionDAO;

  @SuppressWarnings("unchecked")
  public Opml export(User user) {
    final Opml opml = new Opml();
    opml.setFeedType("opml_1.1");
    opml.setTitle(String.format("%s subscriptions in CommaFeed", user.getName()));
    opml.setCreated(new Date());

    final List<FeedCategory> categories = feedCategoryDAO.findAll(user);
    final List<FeedSubscription> subscriptions = feedSubscriptionDAO.findAll(user);

    for (final FeedCategory cat : categories) {
      opml.getOutlines().add(buildCategoryOutline(cat, subscriptions));
    }
    for (final FeedSubscription sub : subscriptions) {
      if (sub.getCategory() == null) {
        opml.getOutlines().add(buildSubscriptionOutline(sub));
      }
    }

    return opml;

  }

  @SuppressWarnings("unchecked")
  private Outline buildCategoryOutline(FeedCategory cat, List<FeedSubscription> subscriptions) {
    final Outline outline = new Outline();
    outline.setText(cat.getName());
    outline.setTitle(cat.getName());

    for (final FeedCategory child : cat.getChildren()) {
      outline.getChildren().add(buildCategoryOutline(child, subscriptions));
    }

    for (final FeedSubscription sub : subscriptions) {
      if (sub.getCategory() != null && sub.getCategory().getId().equals(cat.getId())) {
        outline.getChildren().add(buildSubscriptionOutline(sub));
      }
    }
    return outline;
  }

  @SuppressWarnings("unchecked")
  private Outline buildSubscriptionOutline(FeedSubscription sub) {
    final Outline outline = new Outline();
    outline.setText(sub.getTitle());
    outline.setTitle(sub.getTitle());
    outline.setType("rss");
    outline.getAttributes().add(new Attribute("xmlUrl", sub.getFeed().getUrl()));
    if (sub.getFeed().getLink() != null) {
      outline.getAttributes().add(new Attribute("htmlUrl", sub.getFeed().getLink()));
    }
    return outline;
  }
}
