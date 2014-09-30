package com.commafeed.frontend.rest.resources;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.commafeed.backend.MetricsBean;
import com.commafeed.backend.dao.FeedDAO;
import com.commafeed.backend.feeds.FeedParser;
import com.commafeed.backend.feeds.FeedRefreshTaskGiver;
import com.commafeed.backend.feeds.FetchedFeed;
import com.commafeed.backend.model.Feed;
import com.commafeed.backend.services.ApplicationSettingsService;
import com.google.common.base.Preconditions;

@Path("/push")
public class PubSubHubbubCallbackREST {

  private static Logger log = LoggerFactory.getLogger(PubSubHubbubCallbackREST.class);

  @Context
  HttpServletRequest request;

  @Inject
  private FeedDAO feedDAO;

  @Inject
  private FeedParser parser;

  @Inject
  private FeedRefreshTaskGiver taskGiver;

  @Inject
  private ApplicationSettingsService applicationSettingsService;

  @Inject
  private MetricsBean metricsBean;

  @Path("/callback")
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  public Response verify(@QueryParam("hub.mode") String mode,
      @QueryParam("hub.topic") String topic, @QueryParam("hub.challenge") String challenge,
      @QueryParam("hub.lease_seconds") String leaseSeconds,
      @QueryParam("hub.verify_token") String verifyToken) {
    if (!applicationSettingsService.get().isPubsubhubbub()) {
      return Response.status(Status.FORBIDDEN).entity("pubsubhubbub is disabled").build();
    }

    Preconditions.checkArgument(StringUtils.isNotEmpty(topic));
    Preconditions.checkArgument("subscribe".equals(mode));

    log.debug("confirmation callback received for {}", topic);

    final List<Feed> feeds = feedDAO.findByTopic(topic);

    if (feeds.isEmpty() == false) {
      for (final Feed feed : feeds) {
        log.debug("activated push notifications for {}", feed.getPushTopic());
        feed.setPushLastPing(new Date());
      }
      feedDAO.saveOrUpdate(feeds);
      return Response.ok(challenge).build();
    }
    else {
      log.debug("rejecting callback: no push info for {}", topic);
      return Response.status(Status.NOT_FOUND).build();
    }
  }

  @Path("/callback")
  @POST
  @Consumes({ MediaType.APPLICATION_ATOM_XML, "application/rss+xml" })
  public Response callback() {
    if (!applicationSettingsService.get().isPubsubhubbub()) {
      return Response.status(Status.FORBIDDEN).entity("pubsubhubbub is disabled").build();
    }
    try {
      final byte[] bytes = IOUtils.toByteArray(request.getInputStream());
      final FetchedFeed fetchedFeed = parser.parse(null, bytes);
      final String topic = fetchedFeed.getFeed().getPushTopic();
      if (StringUtils.isNotBlank(topic)) {
        log.debug("content callback received for {}", topic);
        final List<Feed> feeds = feedDAO.findByTopic(topic);
        for (final Feed feed : feeds) {
          log.debug("pushing content to queue for {}", feed.getUrl());
          taskGiver.add(feed);
        }
        metricsBean.pushReceived(feeds.size());
      }
    }
    catch (final Exception e) {
      log.error("Could not parse pubsub callback: " + e.getMessage());
    }
    return Response.ok().build();
  }
}
