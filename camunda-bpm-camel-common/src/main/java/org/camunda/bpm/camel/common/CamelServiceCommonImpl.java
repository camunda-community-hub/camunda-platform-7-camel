package org.camunda.bpm.camel.common;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.impl.DefaultExchange;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.impl.context.Context;
import org.camunda.bpm.engine.impl.pvm.delegate.ActivityExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

import java.util.*;

import static org.camunda.bpm.camel.component.CamundaBpmConstants.CAMUNDA_BPM_PROCESS_INSTANCE_ID;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.CAMUNDA_BPM_BUSINESS_KEY;

public abstract class CamelServiceCommonImpl implements CamelService {

  final Logger log = LoggerFactory.getLogger(this.getClass());

  protected ProcessEngine processEngine;
  protected CamelContext camelContext;

  @Override
  public Object sendTo(String endpointUri) {
    ActivityExecution execution = Context.getExecutionContext().getExecution();
    return sendTo(endpointUri, execution.getVariableNames());
  }

  @Override
  public Object sendTo(String endpointUri, String processVariables) {

    List<String> vars = Arrays.asList(processVariables.split("\\s*,\\s*"));
    return sendTo(endpointUri, vars);
  }

  private Object sendTo(String endpointUri, Collection<String> variables) {
    ActivityExecution execution = (ActivityExecution) Context.getBpmnExecutionContext().getExecution();
    Map<String, Object> variablesToSend = new HashMap<String, Object>();
    for (String var: variables) {
      Object value = execution.getVariable(var);
      if (value == null) {
        throw new IllegalArgumentException("Process variable '" + var + "' no found!");
      }
      variablesToSend.put(var, value);
    }

    log.debug("Sending process variables '{}' as a map to Camel endpoint '{}'", variablesToSend, endpointUri);
    ProducerTemplate producerTemplate = camelContext.createProducerTemplate();
      String businessKey = execution.getBusinessKey();

      Exchange exchange = new DefaultExchange(camelContext);
      exchange.setProperty(CAMUNDA_BPM_PROCESS_INSTANCE_ID, execution.getProcessInstanceId());
      if (businessKey != null) {
          exchange.setProperty(CAMUNDA_BPM_BUSINESS_KEY, businessKey);
      }
      exchange.getIn().setBody(variablesToSend);
      exchange.setPattern(ExchangePattern.InOut);
      Exchange send = producerTemplate.send(endpointUri, exchange);
      return send.getIn().getBody();
  }

  @Required
  public abstract void setProcessEngine(ProcessEngine processEngine);

  @Required
  public abstract void setCamelContext(CamelContext camelContext);
}
