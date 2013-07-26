package org.camunda.bpm.camel.cdi;

import org.apache.camel.CamelContext;
import org.camunda.bpm.camel.common.CamelServiceCommonImpl;
import org.camunda.bpm.engine.ProcessEngine;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;

@Named("camel")
@Stateless
public class CamelServiceImpl extends CamelServiceCommonImpl {

  @Inject
  public void setProcessEngine(ProcessEngine processEngine) {
    this.processEngine = processEngine;
  }

  @Inject
  public void setCamelContext(CamelContext camelContext) {
    this.camelContext = camelContext;
  }
}