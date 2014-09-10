package com.commafeed.backend.rome;

import com.sun.syndication.feed.rss.Description;
import com.sun.syndication.feed.rss.Item;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.impl.ConverterForRSS090;

public class RSS090DescriptionConverter extends ConverterForRSS090 {

  @Override
  protected SyndEntry createSyndEntry(Item item, boolean preserveWireItem) {
    final SyndEntry entry = super.createSyndEntry(item, preserveWireItem);
    final Description desc = item.getDescription();
    if (desc != null) {
      final SyndContentImpl syndDesc = new SyndContentImpl();
      syndDesc.setValue(desc.getValue());
      entry.setDescription(syndDesc);
    }
    return entry;
  }

}
