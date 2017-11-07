package org.camunda.bpm.camel.common;

import static org.camunda.bpm.camel.component.CamundaBpmConstants.CAMUNDA_BPM_CAMEL_BPMN_ERROR;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.EXCHANGE_HEADER_BUSINESS_KEY;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.EXCHANGE_HEADER_CORRELATION_KEY;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.EXCHANGE_HEADER_PROCESS_INSTANCE_ID;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.impl.DefaultExchange;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.impl.context.Context;
import org.camunda.bpm.engine.impl.pvm.delegate.ActivityExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

public abstract class CamelServiceCommonImpl implements CamelService {

  final Logger log = LoggerFactory.getLogger(this.getClass());

  protected ProcessEngine processEngine;
  protected CamelContext camelContext;

  @Override
  public Object sendTo(String endpointUri) {
    return sendTo(endpointUri, null, null);
  }

  @Override
  public Object sendTo(String endpointUri, String processVariables) {
    return sendTo(endpointUri, processVariables, null);
  }

  @Override
  public Object sendTo(String endpointUri, String processVariables,
      String correlationId) {
    Collection<String> vars;
    if (processVariables == null) {
      ActivityExecution execution = Context.getBpmnExecutionContext()
          .getExecution();
      vars = execution.getVariableNames();
    } else if ("".equals(processVariables)) {
      vars = Collections.emptyList();
    } else {
      vars = Arrays.asList(processVariables.split("\\s*,\\s*"));
    }
    return sendToInternal(endpointUri, vars, correlationId);
  }

  private Object sendToInternal(String endpointUri,
      Collection<String> variables, String correlationKey) {
    ActivityExecution execution = (ActivityExecution) Context
        .getBpmnExecutionContext().getExecution();
    Map<String, Object> variablesToSend = new HashMap<String, Object>();
    for (String var : variables) {
      Object value = execution.getVariable(var);
      if (value == null) {
        throw new IllegalArgumentException("Process variable '" + var
            + "' no found!");
      }
      variablesToSend.put(var, value);
    }

    log.debug("Sending process variables '{}' as a map to Camel endpoint '{}'",
        variablesToSend, endpointUri);
    ProducerTemplate producerTemplate = camelContext.createProducerTemplate();
    String businessKey = execution.getBusinessKey();

    Exchange exchange = new DefaultExchange(camelContext);
    exchange.setProperty(EXCHANGE_HEADER_PROCESS_INSTANCE_ID,
        execution.getProcessInstanceId());
    if (businessKey != null) {
      exchange.setProperty(EXCHANGE_HEADER_BUSINESS_KEY, businessKey);
    }
    if (correlationKey != null) {
      exchange.setProperty(EXCHANGE_HEADER_CORRELATION_KEY, correlationKey);
    }
    exchange.getIn().setBody(variablesToSend);
    exchange.setPattern(ExchangePattern.InOut);
    Exchange send = producerTemplate.send(endpointUri, exchange);
    if (send.isFailed()) {
      if (send.getException() != null) {
        throw new BpmnError(CAMUNDA_BPM_CAMEL_BPMN_ERROR, send.getException().getMessage());
      } else {
        throw new BpmnError(CAMUNDA_BPM_CAMEL_BPMN_ERROR);
      }
    }
    return send.getIn().getBody();
  }

  @Required
  public abstract void setProcessEngine(ProcessEngine processEngine);

  @Required
  public abstract void setCamelContext(CamelContext camelContext);
}
