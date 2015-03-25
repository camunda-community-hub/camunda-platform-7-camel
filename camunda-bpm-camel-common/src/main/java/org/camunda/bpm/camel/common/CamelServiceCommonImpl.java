package org.camunda.bpm.camel.common;

import java.util.*;

import org.apache.camel.CamelContext;
import org.apache.camel.ExchangePattern;
import org.apache.camel.ProducerTemplate;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.impl.context.Context;
import org.camunda.bpm.engine.impl.pvm.delegate.ActivityExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.*;

public abstract class CamelServiceCommonImpl implements CamelService {

  final Logger log = LoggerFactory.getLogger(this.getClass());

  protected ProcessEngine processEngine;
  protected CamelContext camelContext;

  @Override
  public Object sendTo(String endpointUri) {
    ActivityExecution execution = Context.getBpmnExecutionContext().getExecution();
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
    Object routeResult = producerTemplate.sendBodyAndProperty(endpointUri, ExchangePattern.InOut,
                                                              variablesToSend, CAMUNDA_BPM_PROCESS_INSTANCE_ID,
                                                              execution.getProcessInstanceId());
    return routeResult;
  }

  @Required
  public abstract void setProcessEngine(ProcessEngine processEngine);

  @Required
  public abstract void setCamelContext(CamelContext camelContext);
}
