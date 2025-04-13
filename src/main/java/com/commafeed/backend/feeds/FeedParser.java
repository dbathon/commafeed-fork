package com.commafeed.backend.feeds;

import com.commafeed.backend.model.Feed;
import com.commafeed.backend.model.FeedEntry;
import com.commafeed.backend.model.FeedEntryContent;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.sun.syndication.feed.synd.*;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import java.io.StringReader;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.jdom.Element;
import org.jdom.Namespace;
import org.xml.sax.InputSource;

public class FeedParser {

  private static final String ATOM_10_URI = "http://www.w3.org/2005/Atom";
  private static final Namespace ATOM_10_NS = Namespace.getNamespace(ATOM_10_URI);

  private static final Date START = new Date(86400000);

  private static final Function<SyndContent, String> CONTENT_TO_STRING = content -> content
      .getValue();

  @SuppressWarnings("unchecked")
  public FetchedFeed parse(String feedUrl, byte[] xml) throws FeedException {
    final FetchedFeed fetchedFeed = new FetchedFeed();
    final Feed feed = fetchedFeed.getFeed();
    final List<FeedEntry> entries = fetchedFeed.getEntries();
    feed.setLastUpdated(new Date());

    try {
      final String encoding = FeedUtils.guessEncoding(xml);
      final String xmlString = FeedUtils.trimInvalidXmlCharacters(new String(xml, encoding));
      if (xmlString == null) {
        throw new FeedException("Input string is null for url " + feedUrl);
      }
      final InputSource source = new InputSource(new StringReader(xmlString));
      final SyndFeed rss = new SyndFeedInput().build(source);
      handleForeignMarkup(rss);

      fetchedFeed.setTitle(rss.getTitle());
      feed.setUrl(feedUrl);
      feed.setLink(rss.getLink());
      final List<SyndEntry> items = rss.getEntries();

      if (items.isEmpty()) {
        throw new FeedException("No items in the feed.");
      }

      for (final SyndEntry item : items) {
        final FeedEntry entry = new FeedEntry();

        String guid = item.getUri();
        if (StringUtils.isBlank(guid)) {
          guid = item.getLink();
        }
        if (StringUtils.isBlank(guid)) {
          // no guid and no link, skip entry
          continue;
        }
        entry.setGuid(FeedUtils.truncate(guid, 2048));
        entry.setGuidHash(DigestUtils.sha1Hex(guid));
        final String url =
            FeedUtils.truncate(FeedUtils.toAbsoluteUrl(item.getLink(), feed.getLink()), 2048);
        if (StringUtils.isBlank(url)) {
          // no url, skip entry
          continue;
        }
        entry.setUrl(url);
        entry.setUpdated(validateDate(getEntryUpdateDate(item), true));

        entry.setAuthor(FeedUtils.truncate(
            FeedUtils.handleContent(item.getAuthor(), feed.getLink(), true), 128));

        final FeedEntryContent content = new FeedEntryContent();
        content.setTitle(FeedUtils.truncate(
            FeedUtils.handleContent(getTitle(item), feed.getLink(), true), 2048));
        content.setContent(FeedUtils.handleContent(getContent(item), feed.getLink(), false));

        final SyndEnclosure enclosure =
            (SyndEnclosure) Iterables.getFirst(item.getEnclosures(), null);
        if (enclosure != null) {
          content.setEnclosureUrl(FeedUtils.truncate(enclosure.getUrl(), 2048));
          content.setEnclosureType(enclosure.getType());
        }
        entry.setContent(content);

        entries.add(entry);
      }
      Date lastEntryDate = null;
      Date publishedDate = validateDate(rss.getPublishedDate(), false);
      if (!entries.isEmpty()) {
        final List<Long> sortedTimestamps = FeedUtils.getSortedTimestamps(entries);
        final Long timestamp = sortedTimestamps.get(0);
        lastEntryDate = new Date(timestamp);
        publishedDate = getFeedPublishedDate(publishedDate, entries);
      }
      feed.setLastPublishedDate(validateDate(publishedDate, true));
      feed.setLastEntryDate(lastEntryDate);

    } catch (final Exception e) {
      throw new FeedException(String.format("Could not parse feed from %s : %s", feedUrl,
          e.getMessage()), e);
    }
    return fetchedFeed;
  }

  /**
   * Adds atom links for rss feeds
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  private void handleForeignMarkup(SyndFeed feed) {
    final Object foreignMarkup = feed.getForeignMarkup();
    if (foreignMarkup == null) {
      return;
    }
    if (foreignMarkup instanceof List) {
      final List elements = (List) foreignMarkup;
      for (final Object object : elements) {
        if (object instanceof Element) {
          final Element element = (Element) object;
          if ("link".equals(element.getName()) && ATOM_10_NS.equals(element.getNamespace())) {
            final SyndLink link = new SyndLinkImpl();
            link.setRel(element.getAttributeValue("rel"));
            link.setHref(element.getAttributeValue("href"));
            feed.getLinks().add(link);
          }
        }
      }
    }
  }

  private Date getFeedPublishedDate(Date publishedDate, List<FeedEntry> entries) {

    for (final FeedEntry entry : entries) {
      if (publishedDate == null || entry.getUpdated().getTime() > publishedDate.getTime()) {
        publishedDate = entry.getUpdated();
      }
    }
    return publishedDate;
  }

  private Date getEntryUpdateDate(SyndEntry item) {
    Date date = item.getUpdatedDate();
    if (date == null) {
      date = item.getPublishedDate();
    }
    if (date == null) {
      date = new Date();
    }
    return date;
  }

  private Date validateDate(Date date, boolean nullToNow) {
    final Date now = new Date();
    if (date == null) {
      return nullToNow ? now : null;
    }
    if (date.before(START) || date.after(now)) {
      return now;
    }
    return date;
  }

  @SuppressWarnings("unchecked")
  private String getContent(SyndEntry item) {
    String content = null;
    if (item.getContents().isEmpty()) {
      content = item.getDescription() == null ? null : item.getDescription().getValue();
    } else {
      content =
          StringUtils.join(Collections2.transform(item.getContents(), CONTENT_TO_STRING),
              SystemUtils.LINE_SEPARATOR);
    }
    return content;
  }

  private String getTitle(SyndEntry item) {
    String title = item.getTitle();
    if (StringUtils.isBlank(title)) {
      final Date date = item.getPublishedDate();
      if (date != null) {
        title = DateFormat.getInstance().format(date);
      } else {
        title = "(no title)";
      }
    }
    return title;
  }

}
