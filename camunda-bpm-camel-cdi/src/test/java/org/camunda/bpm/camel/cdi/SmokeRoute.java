package org.camunda.bpm.camel.cdi;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.cdi.ContextName;

import java.util.logging.Logger;

@ContextName
public class SmokeRoute extends RouteBuilder {

  Logger logger = Logger.getLogger(this.getClass().getName());

  public SmokeRoute() {
    System.out.println(">> SmokeRoute instantiated");
  }

  @Override
  public void configure() throws Exception {
    log.info("Adding route " + this.getClass().getName());
    from("direct:smokeEndpoint")
      .to("log:org.camunda.bpm.camel.cdi?level=INFO&showAll=true&multiline=true")
      .to("mock:endpoint")
    ;
  }
}