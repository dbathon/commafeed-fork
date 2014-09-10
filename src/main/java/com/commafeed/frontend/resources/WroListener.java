package com.commafeed.frontend.resources;

import ro.isdc.wro.config.jmx.WroConfiguration;
import ro.isdc.wro.http.WroServletContextListener;

import com.commafeed.backend.services.ApplicationPropertiesService;

public class WroListener extends WroServletContextListener {

  @Override
  protected WroConfiguration newConfiguration() {
    final WroConfiguration conf = super.newConfiguration();
    final ApplicationPropertiesService properties = ApplicationPropertiesService.get();
    final boolean prod = properties.isProduction();

    conf.setResourceWatcherUpdatePeriod(prod ? 0 : 1);
    conf.setDisableCache(!prod);
    conf.setDebug(!prod);
    return conf;
  }

}
