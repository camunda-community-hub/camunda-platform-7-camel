package org.camunda.bpm.camel.spring;

import org.apache.camel.CamelContext;
import org.camunda.bpm.camel.common.CamelServiceCommonImpl;
import org.camunda.bpm.engine.ProcessEngine;
import org.springframework.beans.factory.annotation.Required;

public class CamelServiceImpl extends CamelServiceCommonImpl {

  @Required
  public void setProcessEngine(ProcessEngine processEngine) {
    this.processEngine = processEngine;
  }

  @Required
  public void setCamelContext(CamelContext camelContext) {
    this.camelContext = camelContext;
  }

}