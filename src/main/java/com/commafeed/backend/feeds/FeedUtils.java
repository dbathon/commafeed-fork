package com.commafeed.backend.feeds;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Document.OutputSettings;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities.EscapeMode;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;
import org.mozilla.universalchardet.UniversalDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.css.sac.InputSource;
import org.w3c.dom.css.CSSStyleDeclaration;

import com.commafeed.backend.model.FeedEntry;
import com.commafeed.backend.model.FeedSubscription;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.steadystate.css.parser.CSSOMParser;

public class FeedUtils {

  protected static Logger log = LoggerFactory.getLogger(FeedUtils.class);

  private static final String ESCAPED_QUESTION_MARK = Pattern.quote("?");
  private static final List<String> ALLOWED_IFRAME_CSS_RULES = Arrays.asList("height", "width",
      "border");
  private static final char[] DISALLOWED_IFRAME_CSS_RULE_CHARACTERS = new char[] { '(', ')' };

  private static final Pattern ALNUM_PATTERN = Pattern.compile("\\p{Alnum}+",
      Pattern.UNICODE_CHARACTER_CLASS);

  private static final Whitelist CONTENT_WHITELIST = buildContentWhitelist();

  private static Whitelist buildContentWhitelist() {
    final Whitelist whitelist = new Whitelist();
    whitelist.addTags("a", "b", "blockquote", "br", "caption", "cite", "code", "col", "colgroup",
        "dd", "div", "dl", "dt", "em", "h1", "h2", "h3", "h4", "h5", "h6", "i", "iframe", "img",
        "li", "ol", "p", "pre", "q", "small", "strike", "strong", "sub", "sup", "table", "tbody",
        "td", "tfoot", "th", "thead", "tr", "u", "ul");

    whitelist.addAttributes("div", "dir");
    whitelist.addAttributes("pre", "dir");
    whitelist.addAttributes("code", "dir");
    whitelist.addAttributes("table", "dir");
    whitelist.addAttributes("p", "dir");
    whitelist.addAttributes("a", "href", "title");
    whitelist.addAttributes("blockquote", "cite");
    whitelist.addAttributes("col", "span", "width");
    whitelist.addAttributes("colgroup", "span", "width");
    whitelist.addAttributes("iframe", "src", "height", "width", "allowfullscreen", "frameborder",
        "style");
    whitelist.addAttributes("img", "align", "alt", "height", "src", "title", "width");
    whitelist.addAttributes("ol", "start", "type");
    whitelist.addAttributes("q", "cite");
    whitelist.addAttributes("table", "border", "bordercolor", "summary", "width");
    whitelist.addAttributes("td", "border", "bordercolor", "abbr", "axis", "colspan", "rowspan",
        "width");
    whitelist.addAttributes("th", "border", "bordercolor", "abbr", "axis", "colspan", "rowspan",
        "scope", "width");
    whitelist.addAttributes("ul", "type");

    whitelist.addProtocols("a", "href", "ftp", "http", "https", "mailto");
    whitelist.addProtocols("blockquote", "cite", "http", "https");
    whitelist.addProtocols("img", "src", "http", "https");
    whitelist.addProtocols("q", "cite", "http", "https");

    whitelist.addEnforcedAttribute("a", "target", "_blank");
    return whitelist;
  }

  public static String truncate(String string, int length) {
    if (string != null) {
      string = string.substring(0, Math.min(length, string.length()));
    }
    return string;
  }

  /**
   * Detect feed encoding by using the declared encoding in the xml processing instruction and by
   * detecting the characters used in the feed
   */
  public static String guessEncoding(byte[] bytes) {
    final String extracted = extractDeclaredEncoding(bytes);
    if (StringUtils.startsWithIgnoreCase(extracted, "iso-8859-")) {
      if (StringUtils.endsWith(extracted, "1") == false) {
        return extracted;
      }
    }
    return detectEncoding(bytes);
  }

  /**
   * Detect encoding by analyzing characters in the array
   */
  public static String detectEncoding(byte[] bytes) {
    final String DEFAULT_ENCODING = "UTF-8";
    final UniversalDetector detector = new UniversalDetector(null);
    detector.handleData(bytes, 0, bytes.length);
    detector.dataEnd();
    String encoding = detector.getDetectedCharset();
    detector.reset();
    if (encoding == null) {
      encoding = DEFAULT_ENCODING;
    }
    else if (encoding.equalsIgnoreCase("ISO-8859-1")) {
      encoding = "windows-1252";
    }
    return encoding;
  }

  /**
   * Normalize the url. The resulting url is not meant to be fetched but rather used as a mean to
   * identify a feed and avoid duplicates
   */
  public static String normalizeURL(String url) {
    if (url == null) {
      return null;
    }
    String normalized = URLCanonicalizer.getCanonicalURL(url);
    if (normalized == null) {
      normalized = url;
    }

    // convert to lower case, the url probably won't work in some cases
    // after that but we don't care we just want to compare urls to avoid
    // duplicates
    normalized = normalized.toLowerCase();

    // store all urls as http
    if (normalized.startsWith("https")) {
      normalized = "http" + normalized.substring(5);
    }

    // remove the www. part
    normalized = normalized.replace("//www.", "//");

    // feedproxy redirects to feedburner
    normalized = normalized.replace("feedproxy.google.com", "feeds.feedburner.com");

    // feedburner feeds have a special treatment
    if (normalized.split(ESCAPED_QUESTION_MARK)[0].contains("feedburner.com")) {
      normalized = normalized.replace("feeds2.feedburner.com", "feeds.feedburner.com");
      normalized = normalized.split(ESCAPED_QUESTION_MARK)[0];
      normalized = StringUtils.removeEnd(normalized, "/");
    }

    return normalized;
  }

  /**
   * Extract the declared encoding from the xml
   */
  public static String extractDeclaredEncoding(byte[] bytes) {
    int index = ArrayUtils.indexOf(bytes, (byte) '>');
    if (index == -1) {
      return null;
    }

    final String pi = new String(ArrayUtils.subarray(bytes, 0, index + 1));
    index = StringUtils.indexOf(pi, "encoding=\"");
    if (index == -1) {
      return null;
    }
    String encoding = pi.substring(index + 10, pi.length());
    encoding = encoding.substring(0, encoding.indexOf('"'));
    return encoding;
  }

  public static String handleContent(String content, String baseUri, boolean keepTextOnly) {
    if (StringUtils.isNotBlank(content)) {
      final Document dirty = Jsoup.parseBodyFragment(content, StringUtils.trimToEmpty(baseUri));
      final Document clean = new Cleaner(CONTENT_WHITELIST).clean(dirty);

      for (final Element e : clean.select("iframe[style]")) {
        final String style = e.attr("style");
        final String escaped = escapeIFrameCss(style);
        e.attr("style", escaped);
      }

      clean.outputSettings(new OutputSettings().escapeMode(EscapeMode.base).prettyPrint(false));
      final Element body = clean.body();
      if (keepTextOnly) {
        content = body.text();
      }
      else {
        content = body.html();
      }
    }
    return content;
  }

  public static String escapeIFrameCss(String orig) {
    final List<String> rules = Lists.newArrayList();
    final CSSOMParser parser = new CSSOMParser();
    try {
      final CSSStyleDeclaration decl =
          parser.parseStyleDeclaration(new InputSource(new StringReader(orig)));

      for (int i = 0; i < decl.getLength(); i++) {
        final String property = decl.item(i);
        final String value = decl.getPropertyValue(property);
        if (StringUtils.isBlank(property) || StringUtils.isBlank(value)) {
          continue;
        }

        if (ALLOWED_IFRAME_CSS_RULES.contains(property)
            && StringUtils.containsNone(value, DISALLOWED_IFRAME_CSS_RULE_CHARACTERS)) {
          rules.add(property + ":" + decl.getPropertyValue(property) + ";");
        }
      }
    }
    catch (final IOException e) {
      log.error(e.getMessage(), e);
    }
    return StringUtils.join(rules, "");
  }

  public static boolean isRTL(FeedEntry entry) {
    String text = entry.getContent().getContent();

    if (StringUtils.isBlank(text)) {
      text = entry.getContent().getTitle();
    }

    if (StringUtils.isBlank(text)) {
      return false;
    }

    text = Jsoup.parse(text).text();
    if (StringUtils.isBlank(text)) {
      return false;
    }

    return BidiUtils.estimateIsRtl(text);
  }

  public static String trimInvalidXmlCharacters(String xml) {
    if (StringUtils.isBlank(xml)) {
      return null;
    }
    final StringBuilder sb = new StringBuilder();

    boolean firstTagFound = false;
    for (int i = 0; i < xml.length(); i++) {
      final char c = xml.charAt(i);

      if (!firstTagFound) {
        if (c == '<') {
          firstTagFound = true;
        }
        else {
          continue;
        }
      }

      if (c >= 32 || c == 9 || c == 10 || c == 13) {
        if (!Character.isHighSurrogate(c) && !Character.isLowSurrogate(c)) {
          sb.append(c);
        }
      }
    }
    return sb.toString();
  }

  /**
   * When there was an error fetching the feed
   */
  public static Date buildDisabledUntil(int errorCount) {
    final Date now = new Date();
    final int retriesBeforeDisable = 3;

    if (errorCount >= retriesBeforeDisable) {
      int disabledHours = errorCount - retriesBeforeDisable + 1;
      disabledHours = Math.min(24 * 7, disabledHours);
      return DateUtils.addHours(now, disabledHours);
    }
    return null;
  }

  public static List<Long> getSortedTimestamps(List<FeedEntry> entries) {
    final List<Long> timestamps = Lists.newArrayList();
    for (final FeedEntry entry : entries) {
      timestamps.add(entry.getUpdated().getTime());
    }
    Collections.sort(timestamps);
    Collections.reverse(timestamps);
    return timestamps;
  }

  public static String removeTrailingSlash(String url) {
    if (url.endsWith("/")) {
      url = url.substring(0, url.length() - 1);
    }
    return url;
  }

  public static String toAbsoluteUrl(String url, String baseUrl) {
    url = StringUtils.trimToNull(StringUtils.normalizeSpace(url));
    if (baseUrl == null || url == null || url.startsWith("http")) {
      return url;
    }

    if (url.startsWith("/") == false) {
      url = "/" + url;
    }

    return baseUrl + url;
  }

  public static String getFaviconUrl(FeedSubscription subscription, String publicUrl) {
    return removeTrailingSlash(publicUrl) + "/rest/feed/favicon/" + subscription.getId();
  }

  public static String proxyImages(String content, String publicUrl, boolean proxyImages) {
    if (!proxyImages) {
      return content;
    }
    if (StringUtils.isBlank(content)) {
      return content;
    }

    final Document doc = Jsoup.parse(content);
    final Elements elements = doc.select("img");
    for (final Element element : elements) {
      final String href = element.attr("src");
      if (href != null) {
        final String proxy =
            removeTrailingSlash(publicUrl) + "/rest/server/proxy?u=" + imageProxyEncoder(href);
        element.attr("src", proxy);
      }
    }

    return doc.body().html();
  }

  public static String rot13(String msg) {
    final StringBuilder message = new StringBuilder();

    for (char c : msg.toCharArray()) {
      if (c >= 'a' && c <= 'm') {
        c += 13;
      }
      else if (c >= 'n' && c <= 'z') {
        c -= 13;
      }
      else if (c >= 'A' && c <= 'M') {
        c += 13;
      }
      else if (c >= 'N' && c <= 'Z') {
        c -= 13;
      }
      message.append(c);
    }

    return message.toString();
  }

  public static String imageProxyEncoder(String url) {
    return Base64.encodeBase64String(rot13(url).getBytes());
  }

  public static String imageProxyDecoder(String code) {
    return rot13(new String(Base64.decodeBase64(code)));
  }

  private static void extractAlnumWords(final String text, Set<String> resultSet) {
    if (!Strings.isNullOrEmpty(text)) {
      final Matcher matcher = ALNUM_PATTERN.matcher(text);
      while (matcher.find()) {
        resultSet.add(matcher.group().toLowerCase(Locale.ROOT));
      }
    }
  }

  public static void extractSearchWords(String input, boolean html, Set<String> resultSet) {
    if (Strings.isNullOrEmpty(input)) {
      return;
    }
    final String text;
    if (html) {
      final Document document = Jsoup.parseBodyFragment(input);
      // also include urls in the result
      for (final Element element : document.select("[href], [src], [cite]")) {
        extractAlnumWords(element.attr("href"), resultSet);
        extractAlnumWords(element.attr("src"), resultSet);
        extractAlnumWords(element.attr("cite"), resultSet);
      }

      text = document.text();
    }
    else {
      text = input;
    }

    extractAlnumWords(text, resultSet);
  }

}
