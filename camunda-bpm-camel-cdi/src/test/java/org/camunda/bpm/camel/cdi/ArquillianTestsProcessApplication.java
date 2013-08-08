package org.camunda.bpm.camel.cdi;

import org.apache.camel.builder.RouteBuilder;
import org.camunda.bpm.application.ProcessApplication;
import org.camunda.bpm.application.ProcessApplicationInterface;
import org.camunda.bpm.application.impl.EjbProcessApplication;
import org.jboss.as.ee.component.InjectionTarget;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.*;
import javax.inject.Inject;
import java.util.logging.Logger;

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

  Logger log = Logger.getLogger(getClass().getName());

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
    log.info(">>");
    log.info(">> Starting the ArquillianTestsProcessApplication ");
    log.info(">>");

    camelContext.addRoute(testRoute);
    camelContext.start();
    deploy();
  }

  @PreDestroy
  public void stop() {
    undeploy();
  }

}