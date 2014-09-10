package com.commafeed.backend.feeds;

import org.junit.Assert;
import org.junit.Test;

public class FeedUtilsTest {

  @Test
  public void testNormalization() {
    final String urla1 = "http://example.com/hello?a=1&b=2";
    final String urla2 = "http://www.example.com/hello?a=1&b=2";
    final String urla3 = "http://EXAmPLe.com/HELLo?a=1&b=2";
    final String urla4 = "http://example.com/hello?b=2&a=1";
    final String urla5 = "https://example.com/hello?a=1&b=2";

    final String urlb1 =
        "http://ftr.fivefilters.org/makefulltextfeed.php?url=http%3A%2F%2Ffeeds.howtogeek.com%2FHowToGeek&max=10&summary=1";
    final String urlb2 =
        "http://ftr.fivefilters.org/makefulltextfeed.php?url=http://feeds.howtogeek.com/HowToGeek&max=10&summary=1";

    final String urlc1 = "http://feeds.feedburner.com/Frandroid";
    final String urlc2 = "http://feeds2.feedburner.com/frandroid";
    final String urlc3 = "http://feedproxy.google.com/frandroid";
    final String urlc4 = "http://feeds.feedburner.com/Frandroid/";
    final String urlc5 = "http://feeds.feedburner.com/Frandroid?format=rss";

    final String urld1 =
        "http://fivefilters.org/content-only/makefulltextfeed.php?url=http://feeds.feedburner.com/Frandroid";
    final String urld2 =
        "http://fivefilters.org/content-only/makefulltextfeed.php?url=http://feeds2.feedburner.com/Frandroid";

    Assert.assertEquals(FeedUtils.normalizeURL(urla1), FeedUtils.normalizeURL(urla2));
    Assert.assertEquals(FeedUtils.normalizeURL(urla1), FeedUtils.normalizeURL(urla3));
    Assert.assertEquals(FeedUtils.normalizeURL(urla1), FeedUtils.normalizeURL(urla4));
    Assert.assertEquals(FeedUtils.normalizeURL(urla1), FeedUtils.normalizeURL(urla5));

    Assert.assertEquals(FeedUtils.normalizeURL(urlb1), FeedUtils.normalizeURL(urlb2));

    Assert.assertEquals(FeedUtils.normalizeURL(urlc1), FeedUtils.normalizeURL(urlc2));
    Assert.assertEquals(FeedUtils.normalizeURL(urlc1), FeedUtils.normalizeURL(urlc3));
    Assert.assertEquals(FeedUtils.normalizeURL(urlc1), FeedUtils.normalizeURL(urlc4));
    Assert.assertEquals(FeedUtils.normalizeURL(urlc1), FeedUtils.normalizeURL(urlc5));

    Assert.assertNotEquals(FeedUtils.normalizeURL(urld1), FeedUtils.normalizeURL(urld2));

  }
}
