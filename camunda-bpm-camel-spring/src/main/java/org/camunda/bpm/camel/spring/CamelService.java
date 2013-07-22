package org.camunda.bpm.camel.spring;

import org.camunda.bpm.engine.impl.pvm.delegate.ActivityExecution;

/**
 *
 * @author Rafael Cordones <rafael@cordones.me>
 */
public interface CamelService {

  public Object sendTo(ActivityExecution execution, String uri, String processVariableForMessageBody);
}
