package com.commafeed.backend.rome;

import com.sun.syndication.feed.rss.Description;
import com.sun.syndication.feed.rss.Item;
import com.sun.syndication.io.impl.RSS090Parser;
import org.jdom.Element;

public class RSS090DescriptionParser extends RSS090Parser {

  @Override
  protected Item parseItem(Element rssRoot, Element eItem) {
    final Item item = super.parseItem(rssRoot, eItem);

    final Element e = eItem.getChild("description", getRSSNamespace());
    if (e != null) {
      final Description desc = new Description();
      desc.setValue(e.getText());
      item.setDescription(desc);
    }

    return item;
  }

}
