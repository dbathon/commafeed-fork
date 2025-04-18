package com.commafeed.frontend.rest.resources;

import com.commafeed.backend.StartupBean;
import com.commafeed.backend.dao.FeedCategoryDAO;
import com.commafeed.backend.dao.FeedEntryStatusDAO;
import com.commafeed.backend.dao.FeedSubscriptionDAO;
import com.commafeed.backend.feeds.*;
import com.commafeed.backend.model.Feed;
import com.commafeed.backend.model.FeedCategory;
import com.commafeed.backend.model.FeedEntryStatus;
import com.commafeed.backend.model.FeedSubscription;
import com.commafeed.backend.model.UserRole.Role;
import com.commafeed.backend.model.UserSettings.ReadingOrder;
import com.commafeed.backend.services.FeedSubscriptionService;
import com.commafeed.frontend.model.Entries;
import com.commafeed.frontend.model.Entry;
import com.commafeed.frontend.model.FeedInfo;
import com.commafeed.frontend.model.Subscription;
import com.commafeed.frontend.model.request.*;
import com.commafeed.frontend.rest.Enums.ReadType;
import com.commafeed.frontend.rest.RestSecurityCheck;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.sun.syndication.feed.opml.Opml;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import com.sun.syndication.io.SyndFeedOutput;
import com.sun.syndication.io.WireFeedOutput;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import java.io.StringWriter;
import java.net.URI;
import java.util.*;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/feed")
@Api(value = "/feed", description = "Operations about feeds")
public class FeedREST extends AbstractREST {

  private static Logger log = LoggerFactory.getLogger(FeedREST.class);

  @Inject
  private StartupBean startupBean;

  @Inject
  private FeedCategoryDAO feedCategoryDAO;

  @Inject
  private FaviconFetcher faviconFetcher;

  @Inject
  private FeedSubscriptionDAO feedSubscriptionDAO;

  @Inject
  private FeedSubscriptionService feedSubscriptionService;

  @Inject
  private FeedFetcher feedFetcher;

  @Inject
  private FeedEntryStatusDAO feedEntryStatusDAO;

  @Inject
  private FeedRefreshTaskGiver taskGiver;

  @Inject
  private OPMLImporter opmlImporter;

  @Inject
  private OPMLExporter opmlExporter;

  @Context
  private HttpServletRequest request;

  @Path("/entries")
  @GET
  @ApiOperation(value = "Get feed entries", notes = "Get a list of feed entries",
      responseClass = "com.commafeed.frontend.model.Entries")
  public Response getFeedEntries(
      @ApiParam(value = "id of the feed", required = true) @QueryParam("id") String id,
      @ApiParam(value = "all entries or only unread ones", allowableValues = "all,unread",
          required = true) @DefaultValue("unread") @QueryParam("readType") ReadType readType,
      @ApiParam(value = "only entries newer than this") @QueryParam("newerThan") Long newerThan,
      @ApiParam(value = "offset for paging") @DefaultValue("0") @QueryParam("offset") int offset,
      @ApiParam(value = "limit for paging, default 20, maximum 50") @DefaultValue("20") @QueryParam("limit") int limit,
      @ApiParam(value = "date ordering", allowableValues = "asc,desc") @QueryParam("order") @DefaultValue("desc") ReadingOrder order) {

    Preconditions.checkNotNull(id);
    Preconditions.checkNotNull(readType);

    limit = Math.min(limit, 50);
    limit = Math.max(0, limit);

    final Entries entries = new Entries();
    final boolean unreadOnly = readType == ReadType.unread;

    final Date newerThanDate = newerThan == null ? null : new Date(Long.valueOf(newerThan));

    final FeedSubscription subscription = feedSubscriptionDAO.findById(getUser(), Long.valueOf(id));
    if (subscription != null) {
      entries.setName(subscription.getTitle());
      entries.setMessage(subscription.getFeed().getMessage());
      entries.setErrorCount(subscription.getFeed().getErrorCount());
      entries.setFeedLink(subscription.getFeed().getLink());

      List<FeedEntryStatus> list = null;
      if (unreadOnly) {
        list =
            feedEntryStatusDAO.findUnreadBySubscriptions(Arrays.asList(subscription),
                newerThanDate, offset, limit + 1, order, true);
      } else {
        list =
            feedEntryStatusDAO.findBySubscriptions(Arrays.asList(subscription), null,
                newerThanDate, offset, limit + 1, order, true);
      }

      for (final FeedEntryStatus status : list) {
        entries.getEntries().add(
            Entry.build(status, applicationSettingsService.get().getPublicUrl(),
                applicationSettingsService.get().isImageProxyEnabled()));
      }

      final boolean hasMore = entries.getEntries().size() > limit;
      if (hasMore) {
        entries.setHasMore(true);
        entries.getEntries().remove(entries.getEntries().size() - 1);
      }
    }

    entries.setTimestamp(System.currentTimeMillis());
    return Response.ok(entries).build();
  }

  @Path("/entriesAsFeed")
  @GET
  @ApiOperation(value = "Get feed entries as a feed", notes = "Get a feed of feed entries")
  @Produces(MediaType.APPLICATION_XML)
  @RestSecurityCheck(value = Role.USER, apiKeyAllowed = true)
  public Response getFeedEntriesAsFeed(
      @ApiParam(value = "id of the feed", required = true) @QueryParam("id") String id) {

    Preconditions.checkNotNull(id);

    final ReadType readType = ReadType.all;
    final ReadingOrder order = ReadingOrder.desc;
    final int offset = 0;
    final int limit = 20;

    final Entries entries =
        (Entries) getFeedEntries(id, readType, null, offset, limit, order).getEntity();

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

  private FeedInfo fetchFeedInternal(String url) {
    FeedInfo info = null;
    url = StringUtils.trimToEmpty(url);
    url = prependHttp(url);
    try {
      final FetchedFeed feed = feedFetcher.fetch(url, true, null, null, null, null);
      info = new FeedInfo();
      info.setUrl(feed.getFeed().getUrl());
      info.setTitle(feed.getTitle());

    } catch (final Exception e) {
      throw new WebApplicationException(e, Response.status(Status.INTERNAL_SERVER_ERROR)
          .entity(e.getMessage()).build());
    }
    return info;
  }

  @POST
  @Path("/fetch")
  @ApiOperation(value = "Fetch a feed", notes = "Fetch a feed by its url",
      responseClass = "com.commafeed.frontend.model.FeedInfo")
  public Response fetchFeed(@ApiParam(value = "feed url", required = true) FeedInfoRequest req) {
    Preconditions.checkNotNull(req);
    Preconditions.checkNotNull(req.getUrl());

    FeedInfo info = null;
    try {
      info = fetchFeedInternal(req.getUrl());
    } catch (final Exception e) {
      return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
    }
    return Response.ok(info).build();
  }

  @Path("/refresh")
  @POST
  @ApiOperation(value = "Queue a feed for refresh",
      notes = "Manually add a feed to the refresh queue")
  public Response queueForRefresh(@ApiParam(value = "Feed id") IDRequest req) {

    Preconditions.checkNotNull(req);
    Preconditions.checkNotNull(req.getId());

    final FeedSubscription sub = feedSubscriptionDAO.findById(getUser(), req.getId());
    if (sub != null) {
      final Feed feed = sub.getFeed();
      feed.setUrgent(true);
      taskGiver.add(feed);
      return Response.ok(Status.OK).build();
    }
    return Response.ok(Status.NOT_FOUND).build();

  }

  @Path("/mark")
  @POST
  @ApiOperation(value = "Mark feed entries",
      notes = "Mark feed entries as read (unread is not supported)")
  public Response markFeedEntries(@ApiParam(value = "Mark request") MarkRequest req) {
    Preconditions.checkNotNull(req);
    Preconditions.checkNotNull(req.getId());

    final Date olderThan = req.getOlderThan() == null ? null : new Date(req.getOlderThan());

    final FeedSubscription subscription =
        feedSubscriptionDAO.findById(getUser(), Long.valueOf(req.getId()));
    if (subscription != null) {
      feedEntryStatusDAO.markSubscriptionEntries(Arrays.asList(subscription), olderThan);
    }
    return Response.ok(Status.OK).build();
  }

  @GET
  @Path("/get/{id}")
  @ApiOperation(value = "", notes = "")
  public Response get(@ApiParam(value = "user id", required = true) @PathParam("id") Long id) {

    Preconditions.checkNotNull(id);
    final FeedSubscription sub = feedSubscriptionDAO.findById(getUser(), id);
    if (sub == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    Long unreadCount = feedSubscriptionService.getUnreadCount(getUser()).get(id);
    if (unreadCount == null) {
      unreadCount = new Long(0);
    }
    return Response.ok(
            Subscription.build(sub, applicationSettingsService.get().getPublicUrl(), unreadCount))
        .build();
  }

  @GET
  @Path("/favicon/{id}")
  @ApiOperation(value = "Fetch a feed's icon", notes = "Fetch a feed's icon")
  public Response getFavicon(@ApiParam(value = "subscription id") @PathParam("id") Long id) {

    Preconditions.checkNotNull(id);
    final FeedSubscription subscription = feedSubscriptionDAO.findById(getUser(), id);
    if (subscription == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    final Feed feed = subscription.getFeed();
    final String url = feed.getLink() != null ? feed.getLink() : feed.getUrl();
    final byte[] icon = faviconFetcher.fetch(url);

    ResponseBuilder builder = null;
    if (icon == null) {
      final String baseUrl =
          FeedUtils.removeTrailingSlash(applicationSettingsService.get().getPublicUrl());
      builder =
          Response.status(Status.MOVED_PERMANENTLY).location(
              URI.create(baseUrl + "/images/default_favicon.gif"));
    } else {
      builder = Response.ok(icon, "image/x-icon");
    }

    final CacheControl cacheControl = new CacheControl();
    cacheControl.setMaxAge(2592000);
    cacheControl.setPrivate(true);
    // trying to replicate "public, max-age=2592000"
    builder.cacheControl(cacheControl);

    final Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.MONTH, 1);
    builder.expires(calendar.getTime());
    builder.lastModified(new Date(startupBean.getStartupTime()));

    return builder.build();
  }

  @POST
  @Path("/subscribe")
  @ApiOperation(value = "Subscribe to a feed", notes = "Subscribe to a feed")
  public Response subscribe(
      @ApiParam(value = "subscription request", required = true) SubscribeRequest req) {
    Preconditions.checkNotNull(req);
    Preconditions.checkNotNull(req.getTitle());
    Preconditions.checkNotNull(req.getUrl());

    String url = prependHttp(req.getUrl());
    try {
      url = fetchFeedInternal(url).getUrl();

      final FeedCategory category =
          CategoryREST.ALL.equals(req.getCategoryId()) ? null : feedCategoryDAO.findById(Long
              .valueOf(req.getCategoryId()));
      final FeedInfo info = fetchFeedInternal(url);
      feedSubscriptionService.subscribe(getUser(), info.getUrl(), req.getTitle(), category);
    } catch (final Exception e) {
      log.info("Failed to subscribe to URL {}: {}", url, e.getMessage());
      return Response.status(Status.SERVICE_UNAVAILABLE)
          .entity("Failed to subscribe to URL " + url + ": " + e.getMessage()).build();
    }
    return Response.ok(Status.OK).build();
  }

  @GET
  @Path("/subscribe")
  @ApiOperation(value = "Subscribe to a feed", notes = "Subscribe to a feed")
  public Response subscribe(
      @ApiParam(value = "feed url", required = true) @QueryParam("url") String url) {

    try {
      Preconditions.checkNotNull(url);

      url = prependHttp(url);
      url = fetchFeedInternal(url).getUrl();

      final FeedInfo info = fetchFeedInternal(url);
      feedSubscriptionService.subscribe(getUser(), info.getUrl(), info.getTitle(), null);
    } catch (final Exception e) {
      log.info("Could not subscribe to url {} : {}", url, e.getMessage());
    }
    return Response.temporaryRedirect(URI.create(applicationSettingsService.get().getPublicUrl()))
        .build();
  }

  private String prependHttp(String url) {
    if (!url.startsWith("http")) {
      url = "http://" + url;
    }
    return url;
  }

  @POST
  @Path("/unsubscribe")
  @ApiOperation(value = "Unsubscribe from a feed", notes = "Unsubscribe from a feed")
  public Response unsubscribe(@ApiParam(required = true) IDRequest req) {
    Preconditions.checkNotNull(req);
    Preconditions.checkNotNull(req.getId());

    final FeedSubscription sub = feedSubscriptionDAO.findById(getUser(), req.getId());
    if (sub != null) {
      feedSubscriptionDAO.delete(sub);
      return Response.ok(Status.OK).build();
    } else {
      return Response.status(Status.NOT_FOUND).build();
    }
  }

  @POST
  @Path("/modify")
  @ApiOperation(value = "Modify a subscription", notes = "Modify a feed subscription")
  public Response modify(
      @ApiParam(value = "subscription id", required = true) FeedModificationRequest req) {
    Preconditions.checkNotNull(req);
    Preconditions.checkNotNull(req.getId());

    final FeedSubscription subscription = feedSubscriptionDAO.findById(getUser(), req.getId());

    if (StringUtils.isNotBlank(req.getName())) {
      subscription.setTitle(req.getName());
    }

    FeedCategory parent = null;
    if (req.getCategoryId() != null && !CategoryREST.ALL.equals(req.getCategoryId())) {
      parent = feedCategoryDAO.findById(getUser(), Long.valueOf(req.getCategoryId()));
    }
    subscription.setCategory(parent);

    if (req.getPosition() != null) {
      final List<FeedSubscription> subs = feedSubscriptionDAO.findByCategory(getUser(), parent);
      Collections.sort(subs, (o1, o2) -> ObjectUtils.compare(o1.getPosition(), o2.getPosition()));

      int existingIndex = -1;
      for (int i = 0; i < subs.size(); i++) {
        if (ObjectUtils.equals(subs.get(i).getId(), subscription.getId())) {
          existingIndex = i;
        }
      }
      if (existingIndex != -1) {
        subs.remove(existingIndex);
      }

      subs.add(Math.min(req.getPosition(), subs.size()), subscription);
      for (int i = 0; i < subs.size(); i++) {
        subs.get(i).setPosition(i);
      }
      feedSubscriptionDAO.saveOrUpdate(subs);
    } else {
      feedSubscriptionDAO.saveOrUpdate(subscription);
    }
    return Response.ok(Status.OK).build();
  }

  @POST
  @Path("/import")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @ApiOperation(value = "OPML import",
      notes = "Import an OPML file, posted as a FORM with the 'file' name")
  public Response importOpml() {

    final String publicUrl = applicationSettingsService.get().getPublicUrl();
    if (StringUtils.isBlank(publicUrl)) {
      throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR)
          .entity("Set the public URL in the admin section.").build());
    }

    try {
      final FileItemFactory factory = new DiskFileItemFactory(1000000, null);
      final ServletFileUpload upload = new ServletFileUpload(factory);
      for (final FileItem item : upload.parseRequest(request)) {
        if ("file".equals(item.getFieldName())) {
          final String opml = IOUtils.toString(item.getInputStream(), "UTF-8");
          if (StringUtils.isNotBlank(opml)) {
            opmlImporter.importOpml(getUser(), opml);
          }
          break;
        }
      }
    } catch (final Exception e) {
      throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR)
          .entity(e.getMessage()).build());
    }
    return Response.temporaryRedirect(URI.create(applicationSettingsService.get().getPublicUrl()))
        .build();
  }

  @GET
  @Path("/export")
  @Produces(MediaType.APPLICATION_XML)
  @ApiOperation(value = "OPML export", notes = "Export an OPML file of the user's subscriptions")
  public Response exportOpml() {
    final Opml opml = opmlExporter.export(getUser());
    final WireFeedOutput output = new WireFeedOutput();
    String opmlString = null;
    try {
      opmlString = output.outputString(opml);
    } catch (final Exception e) {
      return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e).build();
    }
    return Response.ok(opmlString).build();
  }

}
