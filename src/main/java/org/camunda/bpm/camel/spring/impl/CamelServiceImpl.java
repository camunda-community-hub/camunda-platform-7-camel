package org.camunda.bpm.camel.spring.impl;

import org.camunda.bpm.camel.spring.CamelService;
import org.apache.camel.CamelContext;
import org.apache.camel.ExchangePattern;
import org.apache.camel.ProducerTemplate;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.impl.pvm.delegate.ActivityExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

/**
 *
 * @author Rafael Cordones <rafael@cordones.me>
 */
public class CamelServiceImpl implements CamelService {

  final Logger log = LoggerFactory.getLogger(this.getClass());

  ProcessEngine processEngine;
  CamelContext camelContext;

  @Override
  public Object sendToEndpoint(ActivityExecution execution, String uri, String processVariableForMessageBody) {
    log.debug("Process execution:" + execution.toString());

    Object messageBody = execution.getVariable(processVariableForMessageBody);

    log.debug("Sending process variable '{}' to Camel endpoint '{}'", processVariableForMessageBody, uri);

    ProducerTemplate producerTemplate = camelContext.createProducerTemplate();
    Object routeResult = producerTemplate.sendBody(uri, ExchangePattern.InOut, messageBody);

    return routeResult;
  }

  @Required
  public void setProcessEngine(ProcessEngine processEngine) {
    this.processEngine = processEngine;
  }

  @Required
  public void setCamelContext(CamelContext camelContext) {
    this.camelContext = camelContext;
  }

}
