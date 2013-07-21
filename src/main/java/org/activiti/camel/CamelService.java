package org.activiti.camel;

import org.camunda.bpm.engine.impl.pvm.delegate.ActivityExecution;

/**
 *
 * @author Rafael Cordones <rafael@cordones.me>
 */
public interface CamelService {

  public Object sendToEndpoint(ActivityExecution execution, String uri, String processVariableForMessageBody);
}
