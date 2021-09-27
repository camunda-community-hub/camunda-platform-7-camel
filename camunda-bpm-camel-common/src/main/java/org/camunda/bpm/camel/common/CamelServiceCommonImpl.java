package org.camunda.bpm.camel.common;

import static org.camunda.bpm.camel.component.CamundaBpmConstants.EXCHANGE_HEADER_BUSINESS_KEY;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.EXCHANGE_HEADER_CORRELATION_KEY;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.EXCHANGE_HEADER_PROCESS_INSTANCE_ID;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import javax.management.RuntimeErrorException;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.support.DefaultExchange;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.impl.context.Context;
import org.camunda.bpm.engine.impl.pvm.delegate.ActivityExecution;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CamelServiceCommonImpl implements CamelService {

  final Logger log = LoggerFactory.getLogger(this.getClass());

  protected ProcessEngine processEngine;
  protected CamelContext camelContext;

  @Override
  public Object sendTo(String endpointUri) throws Exception {
    return sendTo(endpointUri, null, null);
  }

  @Override
  public Object sendTo(String endpointUri, String processVariables) throws Exception{
    return sendTo(endpointUri, processVariables, null);
  }

  @Override
  public Object sendTo(String endpointUri, String processVariables,
      String correlationId) throws Exception {
    Collection<String> vars;
    if (processVariables == null) {
      vars = new LinkedList<String>();
      ActivityExecution execution = Context.getBpmnExecutionContext()
          .getExecution();
      final Set<String> variableNames = execution.getVariableNames();
      if (variableNames != null) {
        for (String variableName : variableNames) {
          vars.add(variableName + "?");
        }
      }
    } else if ("".equals(processVariables)) {
      vars = Collections.emptyList();
    } else {
      vars = Arrays.asList(processVariables.split("\\s*,\\s*"));
    }
    return sendToInternal(endpointUri, vars, correlationId);
  }

  private Object sendToInternal(String endpointUri,
      Collection<String> variables, String correlationKey) throws Exception {
    ActivityExecution execution = (ActivityExecution) Context
        .getBpmnExecutionContext().getExecution();
    Map<String, Object> variablesToSend = new HashMap<String, Object>();
    for (String var : variables) {
      Object value;
      if (var.endsWith("?")) {
        value = execution.getVariable(var.substring(0, var.length() - 1));
      } else {
        value = execution.getVariable(var);
        if (value == null) {
          throw new IllegalArgumentException("Process variable '" + var
              + "' no found!");
        }
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
    
    // Exception handling
    //    Propogate BpmnError back from camel route, 
    //    all other exceptions will cause workflow to stop as a technical error
    // https://docs.camunda.org/get-started/rpa/error-handling/
    // https://docs.camunda.org/manual/7.15/reference/bpmn20/events/error-events/
    if (null != send.getException()){
      // Explicit BPMN business error, workflow has a chance to handle on boundry event, throw as is
      // Note that this can terminate a process instance if no handling is defined in the model (https://docs.camunda.org/manual/latest/user-guide/process-engine/delegation-code/#throw-bpmn-errors-from-listeners)
      if (send.getException() instanceof BpmnError) throw ((BpmnError)send.getException());
      
      // otherwise simply throw the exception, leads to incident in process instance
      throw send.getException();  
    }

    return send.getIn().getBody();    
  }

  public abstract void setProcessEngine(ProcessEngine processEngine);

  public abstract void setCamelContext(CamelContext camelContext);
}
