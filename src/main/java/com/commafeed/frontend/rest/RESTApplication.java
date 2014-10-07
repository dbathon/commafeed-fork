package com.commafeed.frontend.rest;

import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import com.commafeed.frontend.rest.resources.AdminREST;
import com.commafeed.frontend.rest.resources.CategoryREST;
import com.commafeed.frontend.rest.resources.EntryREST;
import com.commafeed.frontend.rest.resources.FeedREST;
import com.commafeed.frontend.rest.resources.ServerREST;
import com.commafeed.frontend.rest.resources.UserREST;
import com.google.common.collect.Sets;

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
