package org.camunda.bpm.camel.cdi;

import javax.annotation.PostConstruct;
import javax.ejb.Startup;
import javax.enterprise.context.ApplicationScoped;
import java.util.logging.Logger;

@ApplicationScoped
@Startup
public class StartUpBean {
  Logger log = Logger.getLogger(getClass().getName());

  @PostConstruct
  public void onStartup() {
    log.info(">> StartUpBean starting up <<");
  }
}