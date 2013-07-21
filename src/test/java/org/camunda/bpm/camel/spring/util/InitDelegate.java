package org.camunda.bpm.camel.spring.util;

import org.camunda.bpm.camel.spring.CamundaBpmProducer;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

public class InitDelegate implements JavaDelegate {

  @Override
  public void execute(DelegateExecution execution) throws Exception {
    execution.setVariable(CamundaBpmProducer.PROCESS_ID_PROPERTY, execution.getProcessInstanceId());
  }

}
