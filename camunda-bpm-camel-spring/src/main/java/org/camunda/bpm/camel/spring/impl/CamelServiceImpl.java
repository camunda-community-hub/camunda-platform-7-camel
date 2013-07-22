package org.camunda.bpm.camel.spring.impl;

import org.apache.camel.*;
import org.camunda.bpm.camel.spring.CamelService;
import org.camunda.bpm.camel.spring.CamundaBpmProducer;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.impl.pvm.delegate.ActivityExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

import java.util.Map;

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

    log.debug("Sending process variable '{}' as body of message to Camel endpoint '{}'", processVariableForMessageBody, uri);

    ProducerTemplate producerTemplate = camelContext.createProducerTemplate();

    // FIXME: Map<String, Object> processVariables = execution.getVariables();

    Object messageBody = execution.getVariable(processVariableForMessageBody);

    Object routeResult = producerTemplate.sendBodyAndProperty(uri, ExchangePattern.InOut, messageBody,
                                                              CamundaBpmProducer.PROCESS_ID_PROPERTY, execution.getProcessInstanceId());

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
