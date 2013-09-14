package org.camunda.bpm.camel.spring.util;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import static org.camunda.bpm.camel.common.CamundaBpmConstants.*;

public class InitDelegate implements JavaDelegate {

  @Override
  public void execute(DelegateExecution execution) throws Exception {
    execution.setVariable(CAMUNDA_BPM_PROCESS_INSTANCE_ID, execution.getProcessInstanceId());
  }

}
