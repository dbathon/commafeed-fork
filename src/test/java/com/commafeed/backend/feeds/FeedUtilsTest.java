package com.commafeed.backend.feeds;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import com.google.common.collect.ImmutableSet;

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

    assertEquals(FeedUtils.normalizeURL(urla1), FeedUtils.normalizeURL(urla2));
    assertEquals(FeedUtils.normalizeURL(urla1), FeedUtils.normalizeURL(urla3));
    assertEquals(FeedUtils.normalizeURL(urla1), FeedUtils.normalizeURL(urla4));
    assertEquals(FeedUtils.normalizeURL(urla1), FeedUtils.normalizeURL(urla5));

    assertEquals(FeedUtils.normalizeURL(urlb1), FeedUtils.normalizeURL(urlb2));

    assertEquals(FeedUtils.normalizeURL(urlc1), FeedUtils.normalizeURL(urlc2));
    assertEquals(FeedUtils.normalizeURL(urlc1), FeedUtils.normalizeURL(urlc3));
    assertEquals(FeedUtils.normalizeURL(urlc1), FeedUtils.normalizeURL(urlc4));
    assertEquals(FeedUtils.normalizeURL(urlc1), FeedUtils.normalizeURL(urlc5));

    assertNotEquals(FeedUtils.normalizeURL(urld1), FeedUtils.normalizeURL(urld2));
  }

  private void testExtractSearchWords(String input, boolean html, String... expected) {
    final Set<String> resultSet = new HashSet<>();
    FeedUtils.extractSearchWords(input, html, resultSet);
    assertEquals(ImmutableSet.copyOf(expected), resultSet);
  }

  @Test
  public void testExtractSearchWords() {
    testExtractSearchWords(" one tWo2 Three three 123 ", false, "one", "two2", "three", "123");
    testExtractSearchWords(" <!\"§$%&/() '# +*~,.-_:; & a word", false, "a", "word");
    testExtractSearchWords("äÖü abc  漢字 コンピュータ 简化字", false, "abc", "äöü", "漢字", "コンピュータ", "简化字");

    testExtractSearchWords("<a hREf=\"http://example.com/HELLO\" other=\"foo\">text 123 漢字</a>",
        true, "text", "123", "漢字", "http", "example", "com", "hello");
  }

}
