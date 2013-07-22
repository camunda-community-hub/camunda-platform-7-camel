package org.camunda.bpm.camel.spring;

import org.camunda.bpm.engine.impl.pvm.delegate.ActivityExecution;

import java.util.Map;

/**
 *
 * @author Rafael Cordones <rafael@cordones.me>
 */
public interface CamelService {

  public Object sendTo(ActivityExecution execution, String uri, String processVariableForMessageBody);
  public Object sendTo(ActivityExecution execution, String uri, Map<String, Object> processVariables);
}
