package org.camunda.bpm.camel.common;

import org.apache.camel.CamelContext;
import org.apache.camel.ExchangePattern;
import org.apache.camel.ProducerTemplate;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.impl.pvm.delegate.ActivityExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import static org.camunda.bpm.camel.common.CamundaBpmConstants.*;

public abstract class CamelServiceCommonImpl implements CamelService {

  final Logger log = LoggerFactory.getLogger(this.getClass());

  protected ProcessEngine processEngine;
  protected CamelContext camelContext;

  @Override
  public Object sendTo(ActivityExecution execution, String uri, String processVariableForMessageBody) {
    log.debug("Process execution:" + execution.toString());

    log.debug("Sending process variable '{}' as body of message to Camel endpoint '{}'", processVariableForMessageBody, uri);

    ProducerTemplate producerTemplate = camelContext.createProducerTemplate();

    // FIXME: Map<String, Object> processVariables = execution.getVariables();

    Object messageBody = execution.getVariable(processVariableForMessageBody);

    Object routeResult = producerTemplate.sendBodyAndProperty(uri, ExchangePattern.InOut, messageBody,
      CAMUNDA_BPM_PROCESS_INSTANCE_ID, execution.getProcessInstanceId());

    return routeResult;
  }

  @Required
  public abstract void setProcessEngine(ProcessEngine processEngine);

  @Required
  public abstract void setCamelContext(CamelContext camelContext);
}
