package org.camunda.bpm.camel.cdi;

import org.apache.camel.builder.RouteBuilder;

import java.util.logging.Logger;

public class SendToCamelRoute extends RouteBuilder {

  Logger logger = Logger.getLogger(SmokeRoute.class.getName());

  public SendToCamelRoute() {
    System.out.println(">> SendToCamelRoute instantiated");
  }

  @Override
  public void configure() throws Exception {

    from("direct:sendToCamelServiceTask")
      .to("log:org.camunda.bpm.camel.cdi?level=INFO&showAll=true&multiline=true")
      .to("mock:endpoint")
    ;
  }
}