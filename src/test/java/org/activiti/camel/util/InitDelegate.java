package org.activiti.camel.util;

import org.activiti.camel.ActivitiProducer;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

public class InitDelegate implements JavaDelegate {

  @Override
  public void execute(DelegateExecution execution) throws Exception {
    execution.setVariable(ActivitiProducer.PROCESS_ID_PROPERTY, execution.getProcessInstanceId());
  }

}
