package org.camunda.bpm.camel.cdi;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.cdi.CdiCamelContext;
import org.camunda.bpm.camel.common.CamundaBpmComponent;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import java.util.logging.Logger;

/**
 * Camel context for CDI tests. 
 * 
 * Since the @Startup annotation is *lazily* initalized we do not have a guarantedd that the Camel context 
 * (components, ...) will be ready when needed. This is why we use the ArquillianTestsProcessApplication.class 
 * to make sure that the CamelContextBootstrap is *really* initialized at application start.
 * 
 * Follow this link for background:
 * http://rmannibucau.wordpress.com/2012/12/11/ensure-some-applicationscoped-beans-are-eagerly-initialized-with-javaee/
 */
@Singleton
@Startup
public class CamelContextBootstrap {

  Logger log = Logger.getLogger(getClass().getName());

  @Inject
  CdiCamelContext camelCtx;

  @PostConstruct
  public void init() throws Exception {
    log.info(">>");
    log.info(">> Starting Apache Camel's context");
    log.info(">>");

    log.info(">>");
    log.info(">> Registering camunda BPM component in Camel context");
    log.info(">>");
    CamundaBpmComponent component = new CamundaBpmComponent();
    component.setCamelContext(camelCtx);
    camelCtx.addComponent("camunda-bpm", component);
  }

  public void addRoute(RouteBuilder route) throws Exception {
    log.info(">>");
    log.info(">> Registering Camel route");
    log.info(">>");
    camelCtx.addRoutes(route);
  }

  public void start() {
    camelCtx.start();
    log.info(">>");
    log.info(">> Camel context started");
    log.info(">>");
  }

  @PreDestroy
  public void stop() throws Exception {
    camelCtx.stop();
    log.info(">> Camel context stopped");
  }

  public CamelContext getCamelContext() {
    return this.camelCtx;
  }

}
