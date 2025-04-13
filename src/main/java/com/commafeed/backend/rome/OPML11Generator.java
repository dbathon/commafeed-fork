package com.commafeed.backend.rome;

import com.sun.syndication.feed.opml.Opml;
import org.jdom.Element;

public class OPML11Generator extends com.sun.syndication.io.impl.OPML10Generator {

  public OPML11Generator() {
    super("opml_1.1");
  }

  @Override
  protected Element generateHead(Opml opml) {
    final Element head = new Element("head");
    addNotNullSimpleElement(head, "title", opml.getTitle());
    return head;
  }
}
