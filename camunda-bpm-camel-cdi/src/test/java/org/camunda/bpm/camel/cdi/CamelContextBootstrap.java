package org.camunda.bpm.camel.cdi;

import org.apache.camel.CamelContext;
import org.apache.camel.cdi.CdiCamelContext;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Startup;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.logging.Logger;

/**
 * Camel context for CDI tests
 */
@Singleton
@Startup
public class CamelContextBootstrap {

  Logger log = Logger.getLogger(getClass().getName());

  @Inject
  CdiCamelContext camelCtx;

  @PostConstruct
  public void init() throws Exception {
    log.info(">> Starting Apache Camel's context: ...");

    // Add the Camel routes
    //camelCtx.addRoutes(tweetRoute);

    // Start Camel context
    camelCtx.start();

    log.info(">> Camel context started and routes started.");
  }

  @PreDestroy
  public void stop() throws Exception {
    camelCtx.stop();
  }

  public CamelContext getCamelContext() {
    return this.camelCtx;
  }

}
