package com.commafeed.frontend.rest;

import com.commafeed.frontend.rest.resources.*;
import com.google.common.collect.Sets;
import java.util.Set;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

@ApplicationPath("/rest")
public class RESTApplication extends Application {

  @Override
  public Set<Class<?>> getClasses() {
    final Set<Class<?>> set = Sets.newHashSet();
    set.add(JsonProvider.class);

    set.add(EntryREST.class);
    set.add(FeedREST.class);
    set.add(CategoryREST.class);
    set.add(UserREST.class);
    set.add(ServerREST.class);
    set.add(AdminREST.class);

    return set;
  }
}
