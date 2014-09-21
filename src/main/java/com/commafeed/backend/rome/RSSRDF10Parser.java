package com.commafeed.backend.rome;

import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;

import com.google.common.collect.Lists;
import com.sun.syndication.io.impl.RSS10Parser;

public class RSSRDF10Parser extends RSS10Parser {

  private static final String RSS_URI = "http://purl.org/rss/1.0/";
  private static final Namespace RSS_NS = Namespace.getNamespace(RSS_URI);

  public RSSRDF10Parser() {
    super("rss_1.0", RSS_NS);
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Override
  public boolean isMyType(Document document) {
    boolean ok = false;

    final Element rssRoot = document.getRootElement();
    final Namespace defaultNS = rssRoot.getNamespace();
    final List additionalNSs = Lists.newArrayList(rssRoot.getAdditionalNamespaces());
    final List<Element> children = rssRoot.getChildren();
    if (children != null && !children.isEmpty()) {
      final Element child = children.get(0);
      additionalNSs.add(child.getNamespace());
      additionalNSs.addAll(child.getAdditionalNamespaces());
    }

    ok = defaultNS != null && defaultNS.equals(getRDFNamespace());
    if (ok) {
      if (additionalNSs == null) {
        ok = false;
      }
      else {
        ok = false;
        for (int i = 0; !ok && i < additionalNSs.size(); i++) {
          ok = getRSSNamespace().equals(additionalNSs.get(i));
        }
      }
    }
    return ok;
  }
}
