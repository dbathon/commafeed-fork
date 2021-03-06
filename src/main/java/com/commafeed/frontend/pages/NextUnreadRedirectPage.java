package com.commafeed.frontend.pages;

import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.commafeed.backend.dao.FeedCategoryDAO;
import com.commafeed.backend.dao.FeedEntryStatusDAO;
import com.commafeed.backend.dao.FeedSubscriptionDAO;
import com.commafeed.backend.model.FeedCategory;
import com.commafeed.backend.model.FeedEntryStatus;
import com.commafeed.backend.model.FeedSubscription;
import com.commafeed.backend.model.User;
import com.commafeed.backend.model.UserRole.Role;
import com.commafeed.backend.model.UserSettings.ReadingOrder;
import com.commafeed.frontend.CommaFeedSession;
import com.commafeed.frontend.rest.resources.CategoryREST;
import com.google.common.collect.Iterables;

@SuppressWarnings("serial")
@PagesSecurityCheck(Role.USER)
public class NextUnreadRedirectPage extends WebPage {

  public static final String PARAM_CATEGORYID = "category";
  public static final String PARAM_READINGORDER = "order";

  @Inject
  private FeedCategoryDAO feedCategoryDAO;

  @Inject
  private FeedEntryStatusDAO feedEntryStatusDAO;

  @Inject
  private FeedSubscriptionDAO feedSubscriptionDAO;

  public NextUnreadRedirectPage(PageParameters params) {
    final String categoryId = params.get(PARAM_CATEGORYID).toString();
    final String orderParam = params.get(PARAM_READINGORDER).toString();

    final User user = CommaFeedSession.get().getUser();
    ReadingOrder order = ReadingOrder.desc;

    if (StringUtils.equals(orderParam, "asc")) {
      order = ReadingOrder.asc;
    }

    List<FeedEntryStatus> statuses = null;
    if (StringUtils.isBlank(categoryId) || CategoryREST.ALL.equals(categoryId)) {
      statuses = feedEntryStatusDAO.findAllUnread(user, null, 0, 1, order, true);
    }
    else {
      final FeedCategory category = feedCategoryDAO.findById(user, Long.valueOf(categoryId));
      if (category != null) {
        final List<FeedCategory> children =
            feedCategoryDAO.findAllChildrenCategories(user, category);
        final List<FeedSubscription> subscriptions =
            feedSubscriptionDAO.findByCategories(user, children);
        statuses =
            feedEntryStatusDAO.findUnreadBySubscriptions(subscriptions, null, 0, 1, order, true);
      }
    }

    if (statuses == null || statuses.isEmpty()) {
      setResponsePage(HomePage.class);
    }
    else {
      final FeedEntryStatus status = Iterables.getFirst(statuses, null);
      final String url = status.getEntry().getUrl();
      status.setRead(true);
      feedEntryStatusDAO.saveOrUpdate(status);
      throw new RedirectToUrlException(url);
    }
  }

}
