package com.commafeed.frontend.rest.resources;

import com.commafeed.backend.dao.FeedCategoryDAO;
import com.commafeed.backend.dao.FeedEntryStatusDAO;
import com.commafeed.backend.dao.FeedSubscriptionDAO;
import com.commafeed.backend.model.FeedCategory;
import com.commafeed.backend.model.FeedEntryStatus;
import com.commafeed.backend.model.FeedSubscription;
import com.commafeed.backend.model.User;
import com.commafeed.backend.model.UserRole.Role;
import com.commafeed.backend.model.UserSettings.ReadingOrder;
import com.commafeed.backend.services.FeedSubscriptionService;
import com.commafeed.frontend.model.*;
import com.commafeed.frontend.model.request.*;
import com.commafeed.frontend.rest.Enums.ReadType;
import com.commafeed.frontend.rest.RestSecurityCheck;
import com.google.common.base.Preconditions;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import com.sun.syndication.io.SyndFeedOutput;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/category")
@Api(value = "/category", description = "Operations about user categories")
public class CategoryREST extends AbstractREST {

  private static Logger log = LoggerFactory.getLogger(CategoryREST.class);

  public static final String ALL = "all";
  public static final String STARRED = "starred";

  @Inject
  private FeedEntryStatusDAO feedEntryStatusDAO;

  @Inject
  private FeedCategoryDAO feedCategoryDAO;

  @Inject
  private FeedSubscriptionDAO feedSubscriptionDAO;

  @Inject
  private FeedSubscriptionService feedSubscriptionService;

  @Path("/entries")
  @GET
  @ApiOperation(value = "Get category entries", notes = "Get a list of category entries",
      responseClass = "com.commafeed.frontend.model.Entries")
  public Response getCategoryEntries(
      @ApiParam(value = "id of the category, 'all' or 'starred'", required = true) @QueryParam("id") String id,
      @ApiParam(value = "all entries or only unread ones", allowableValues = "all,unread",
          required = true) @DefaultValue("unread") @QueryParam("readType") ReadType readType,
      @ApiParam(value = "only entries newer than this") @QueryParam("newerThan") Long newerThan,
      @ApiParam(value = "offset for paging") @DefaultValue("0") @QueryParam("offset") int offset,
      @ApiParam(value = "limit for paging, default 20, maximum 50") @DefaultValue("20") @QueryParam("limit") int limit,
      @ApiParam(value = "date ordering", allowableValues = "asc,desc") @QueryParam("order") @DefaultValue("desc") ReadingOrder order) {

    Preconditions.checkNotNull(readType);
    limit = Math.min(limit, 50);
    limit = Math.max(0, limit);

    final Entries entries = new Entries();
    final boolean unreadOnly = readType == ReadType.unread;
    if (StringUtils.isBlank(id)) {
      id = ALL;
    }

    final Date newerThanDate = newerThan == null ? null : new Date(Long.valueOf(newerThan));

    if (ALL.equals(id)) {
      entries.setName("All");
      List<FeedEntryStatus> list = null;
      final List<FeedSubscription> subscriptions = feedSubscriptionDAO.findAll(getUser());
      if (unreadOnly) {
        list =
            feedEntryStatusDAO.findAllUnread(getUser(), newerThanDate, offset, limit + 1, order,
                true);
      } else {
        list =
            feedEntryStatusDAO.findBySubscriptions(subscriptions, null, newerThanDate, offset,
                limit + 1, order, true);
      }
      for (final FeedEntryStatus status : list) {
        entries.getEntries().add(
            Entry.build(status, applicationSettingsService.get().getPublicUrl(),
                applicationSettingsService.get().isImageProxyEnabled()));
      }

    } else if (STARRED.equals(id)) {
      entries.setName("Starred");
      final List<FeedEntryStatus> starred =
          feedEntryStatusDAO.findStarred(getUser(), newerThanDate, offset, limit + 1, order, true);
      for (final FeedEntryStatus status : starred) {
        entries.getEntries().add(
            Entry.build(status, applicationSettingsService.get().getPublicUrl(),
                applicationSettingsService.get().isImageProxyEnabled()));
      }
    } else {
      final FeedCategory parent = feedCategoryDAO.findById(getUser(), Long.valueOf(id));
      if (parent != null) {
        final List<FeedCategory> categories =
            feedCategoryDAO.findAllChildrenCategories(getUser(), parent);
        final List<FeedSubscription> subs =
            feedSubscriptionDAO.findByCategories(getUser(), categories);
        List<FeedEntryStatus> list = null;
        if (unreadOnly) {
          list =
              feedEntryStatusDAO.findUnreadBySubscriptions(subs, newerThanDate, offset, limit + 1,
                  order, true);
        } else {
          list =
              feedEntryStatusDAO.findBySubscriptions(subs, null, newerThanDate, offset, limit + 1,
                  order, true);
        }
        for (final FeedEntryStatus status : list) {
          entries.getEntries().add(
              Entry.build(status, applicationSettingsService.get().getPublicUrl(),
                  applicationSettingsService.get().isImageProxyEnabled()));
        }
        entries.setName(parent.getName());
      }

    }

    final boolean hasMore = entries.getEntries().size() > limit;
    if (hasMore) {
      entries.setHasMore(true);
      entries.getEntries().remove(entries.getEntries().size() - 1);
    }

    entries.setTimestamp(System.currentTimeMillis());
    return Response.ok(entries).build();
  }

  @Path("/entriesAsFeed")
  @GET
  @ApiOperation(value = "Get category entries as feed", notes = "Get a feed of category entries")
  @Produces(MediaType.APPLICATION_XML)
  @RestSecurityCheck(value = Role.USER, apiKeyAllowed = true)
  public Response getCategoryEntriesAsFeed(@ApiParam(
      value = "id of the category, 'all' or 'starred'", required = true) @QueryParam("id") String id) {

    Preconditions.checkNotNull(id);

    final ReadType readType = ReadType.all;
    final ReadingOrder order = ReadingOrder.desc;
    final int offset = 0;
    final int limit = 20;

    final Entries entries =
        (Entries) getCategoryEntries(id, readType, null, offset, limit, order).getEntity();

    final SyndFeed feed = new SyndFeedImpl();
    feed.setFeedType("rss_2.0");
    feed.setTitle("CommaFeed - " + entries.getName());
    feed.setDescription("CommaFeed - " + entries.getName());
    final String publicUrl = applicationSettingsService.get().getPublicUrl();
    feed.setLink(publicUrl);

    final List<SyndEntry> children = Lists.newArrayList();
    for (final Entry entry : entries.getEntries()) {
      children.add(entry.asRss());
    }
    feed.setEntries(children);

    final SyndFeedOutput output = new SyndFeedOutput();
    final StringWriter writer = new StringWriter();
    try {
      output.output(feed, writer);
    } catch (final Exception e) {
      writer.write("Could not get feed information");
      log.error(e.getMessage(), e);
    }
    return Response.ok(writer.toString()).build();
  }

  @Path("/mark")
  @POST
  @ApiOperation(value = "Mark category entries",
      notes = "Mark feed entries of this category as read")
  public Response markCategoryEntries(
      @ApiParam(value = "category id, or 'all'", required = true) MarkRequest req) {
    Preconditions.checkNotNull(req);
    Preconditions.checkNotNull(req.getId());

    final Date olderThan = req.getOlderThan() == null ? null : new Date(req.getOlderThan());

    if (ALL.equals(req.getId())) {
      feedEntryStatusDAO.markAllEntries(getUser(), olderThan);
    } else if (STARRED.equals(req.getId())) {
      feedEntryStatusDAO.markStarredEntries(getUser(), olderThan);
    } else {
      final FeedCategory parent = feedCategoryDAO.findById(getUser(), Long.valueOf(req.getId()));
      final List<FeedCategory> categories =
          feedCategoryDAO.findAllChildrenCategories(getUser(), parent);
      final List<FeedSubscription> subs =
          feedSubscriptionDAO.findByCategories(getUser(), categories);
      feedEntryStatusDAO.markSubscriptionEntries(subs, olderThan);
    }
    return Response.ok(Status.OK).build();
  }

  @Path("/add")
  @POST
  @ApiOperation(value = "Add a category", notes = "Add a new feed category")
  public Response addCategory(@ApiParam(required = true) AddCategoryRequest req) {
    Preconditions.checkNotNull(req);
    Preconditions.checkNotNull(req.getName());

    final FeedCategory cat = new FeedCategory();
    cat.setName(req.getName());
    cat.setUser(getUser());
    cat.setPosition(0);
    final String parentId = req.getParentId();
    if (parentId != null && !ALL.equals(parentId)) {
      final FeedCategory parent = new FeedCategory();
      parent.setId(Long.valueOf(parentId));
      cat.setParent(parent);
    }
    feedCategoryDAO.saveOrUpdate(cat);
    return Response.ok().build();
  }

  @POST
  @Path("/delete")
  @ApiOperation(value = "Delete a category", notes = "Delete an existing feed category")
  public Response deleteCategory(@ApiParam(required = true) IDRequest req) {

    Preconditions.checkNotNull(req);
    Preconditions.checkNotNull(req.getId());

    final FeedCategory cat = feedCategoryDAO.findById(getUser(), req.getId());
    if (cat != null) {
      final List<FeedSubscription> subs = feedSubscriptionDAO.findByCategory(getUser(), cat);
      for (final FeedSubscription sub : subs) {
        sub.setCategory(null);
      }
      feedSubscriptionDAO.saveOrUpdate(subs);
      final List<FeedCategory> categories =
          feedCategoryDAO.findAllChildrenCategories(getUser(), cat);
      for (final FeedCategory child : categories) {
        if (!child.getId().equals(cat.getId()) && child.getParent().getId().equals(cat.getId())) {
          child.setParent(null);
        }
      }
      feedCategoryDAO.saveOrUpdate(categories);

      feedCategoryDAO.delete(cat);
      return Response.ok().build();
    } else {
      return Response.status(Status.NOT_FOUND).build();
    }
  }

  @POST
  @Path("/modify")
  @ApiOperation(value = "Rename a category", notes = "Rename an existing feed category")
  public Response modifyCategory(@ApiParam(required = true) CategoryModificationRequest req) {
    Preconditions.checkNotNull(req);
    Preconditions.checkNotNull(req.getId());

    final FeedCategory category = feedCategoryDAO.findById(getUser(), req.getId());

    if (StringUtils.isNotBlank(req.getName())) {
      category.setName(req.getName());
    }

    FeedCategory parent = null;
    if (req.getParentId() != null && !CategoryREST.ALL.equals(req.getParentId())
        && !StringUtils.equals(req.getParentId(), String.valueOf(req.getId()))) {
      parent = feedCategoryDAO.findById(getUser(), Long.valueOf(req.getParentId()));
    }
    category.setParent(parent);

    if (req.getPosition() != null) {
      final List<FeedCategory> categories = feedCategoryDAO.findByParent(getUser(), parent);
      Collections.sort(categories,
          (o1, o2) -> ObjectUtils.compare(o1.getPosition(), o2.getPosition()));

      int existingIndex = -1;
      for (int i = 0; i < categories.size(); i++) {
        if (ObjectUtils.equals(categories.get(i).getId(), category.getId())) {
          existingIndex = i;
        }
      }
      if (existingIndex != -1) {
        categories.remove(existingIndex);
      }

      categories.add(Math.min(req.getPosition(), categories.size()), category);
      for (int i = 0; i < categories.size(); i++) {
        categories.get(i).setPosition(i);
      }
      feedCategoryDAO.saveOrUpdate(categories);
    } else {
      feedCategoryDAO.saveOrUpdate(category);
    }

    feedCategoryDAO.saveOrUpdate(category);
    return Response.ok(Status.OK).build();
  }

  @POST
  @Path("/collapse")
  @ApiOperation(value = "Collapse a category",
      notes = "Save collapsed or expanded status for a category")
  public Response collapse(@ApiParam(required = true) CollapseRequest req) {
    Preconditions.checkNotNull(req);
    Preconditions.checkNotNull(req.getId());

    final FeedCategory category = feedCategoryDAO.findById(getUser(), Long.valueOf(req.getId()));
    if (category == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    category.setCollapsed(req.isCollapse());
    feedCategoryDAO.saveOrUpdate(category);
    return Response.ok(Status.OK).build();
  }

  @GET
  @Path("/unreadCount")
  @ApiOperation(value = "Get unread count for feed subscriptions",
      responseClass = "List[com.commafeed.frontend.model.UnreadCount]")
  public Response getUnreadCount() {
    final List<UnreadCount> list = Lists.newArrayList();
    final Map<Long, Long> unreadCount = feedSubscriptionService.getUnreadCount(getUser());
    for (final Map.Entry<Long, Long> e : unreadCount.entrySet()) {
      list.add(new UnreadCount(e.getKey(), e.getValue()));
    }
    return Response.ok(list).build();
  }

  @GET
  @Path("/get")
  @ApiOperation(value = "Get feed categories",
      notes = "Get all categories and subscriptions of the user",
      responseClass = "com.commafeed.frontend.model.Category")
  public Response getSubscriptions() {
    final User user = getUser();

    final List<FeedCategory> categories = feedCategoryDAO.findAll(user);
    final List<FeedSubscription> subscriptions = feedSubscriptionDAO.findAll(getUser());
    final Map<Long, Long> unreadCount = feedSubscriptionService.getUnreadCount(getUser());

    final Category root = buildCategory(null, categories, subscriptions, unreadCount);
    root.setId("all");
    root.setName("All");
    return Response.ok(root).build();
  }

  private Category buildCategory(Long id, List<FeedCategory> categories,
                                 List<FeedSubscription> subscriptions, Map<Long, Long> unreadCount) {
    final Category category = new Category();
    category.setId(String.valueOf(id));
    category.setExpanded(true);

    for (final FeedCategory c : categories) {
      if ((id == null && c.getParent() == null)
          || (c.getParent() != null && ObjectUtils.equals(c.getParent().getId(), id))) {
        final Category child = buildCategory(c.getId(), categories, subscriptions, unreadCount);
        child.setId(String.valueOf(c.getId()));
        child.setName(c.getName());
        child.setPosition(c.getPosition());
        if (c.getParent() != null && c.getParent().getId() != null) {
          child.setParentId(String.valueOf(c.getParent().getId()));
        }
        child.setExpanded(!c.isCollapsed());
        category.getChildren().add(child);
      }
    }
    Collections.sort(
        category.getChildren(),
        (o1, o2) -> ComparisonChain.start()
            .compare(o1.getPosition(), o2.getPosition(), Ordering.natural().nullsFirst())
            .compare(o1.getId(), o2.getId()).result());

    for (final FeedSubscription subscription : subscriptions) {
      if ((id == null && subscription.getCategory() == null)
          || (subscription.getCategory() != null && ObjectUtils.equals(subscription.getCategory()
          .getId(), id))) {
        final Long size = unreadCount.get(subscription.getId());
        final long unread = size == null ? 0 : size;
        final Subscription sub =
            Subscription.build(subscription, applicationSettingsService.get().getPublicUrl(),
                unread);
        category.getFeeds().add(sub);
      }
    }
    Collections.sort(
        category.getFeeds(),
        (o1, o2) -> ComparisonChain.start()
            .compare(o1.getPosition(), o2.getPosition(), Ordering.natural().nullsFirst())
            .compare(o1.getId(), o2.getId()).result());
    return category;
  }

}
