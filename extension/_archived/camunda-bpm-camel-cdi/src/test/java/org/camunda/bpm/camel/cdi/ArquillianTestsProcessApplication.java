package org.camunda.bpm.camel.cdi;

import org.apache.camel.builder.RouteBuilder;
import org.camunda.bpm.application.ProcessApplication;
import org.camunda.bpm.application.ProcessApplicationInterface;
import org.camunda.bpm.application.impl.EjbProcessApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.*;
import javax.inject.Inject;

/**
 * We need this class because @Startup annotations are lazily instantiated and not *really* initalized at
 * startup. We use this class to make sure that the CamelContextBootstrap is initialized at startup.
 */
@Singleton
@Startup
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@TransactionAttribute(TransactionAttributeType.REQUIRED)
@ProcessApplication
@Local(ProcessApplicationInterface.class)
public class ArquillianTestsProcessApplication extends EjbProcessApplication {

  private static final Logger LOG = LoggerFactory.getLogger(ArquillianTestsProcessApplication.class);

  @Inject
  CamelContextBootstrap camelContext;

  /*
   * Every integration test needs to provide a method that will return the Camel route
   * to be use for testing with a @Produces annotation
   */
  @Inject
  RouteBuilder testRoute;

  @PostConstruct
  public void start() throws Exception {
    LOG.info(">>");
    LOG.info(">> Starting the ArquillianTestsProcessApplication ");
    LOG.info(">>");

    camelContext.addRoute(testRoute);
    camelContext.start();
    deploy();
  }

  @PreDestroy
  public void stop() {
    undeploy();
  }

}