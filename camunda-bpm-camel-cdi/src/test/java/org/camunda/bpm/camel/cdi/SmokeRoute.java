package org.camunda.bpm.camel.cdi;

import org.apache.camel.builder.RouteBuilder;

import java.util.logging.Logger;

public class SmokeRoute extends RouteBuilder {

  Logger logger = Logger.getLogger(this.getClass().getName());

  public SmokeRoute() {
    System.out.println(">> SmokeRoute instantiated");
  }

  @Override
  public void configure() throws Exception {

    from("direct:smokeEndpoint")
      .to("log:org.camunda.bpm.camel.cdi?level=INFO&showAll=true&multiline=true")
      .to("mock:endpoint")
    ;
  }
}