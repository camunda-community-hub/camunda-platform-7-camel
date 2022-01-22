package org.camunda.bpm.camel.spring;

import org.camunda.bpm.camel.component.CamundaBpmComponent;
import org.camunda.bpm.engine.ProcessEngine;
import org.springframework.beans.factory.annotation.Autowired;

public class SpringCamundaBpmComponent extends CamundaBpmComponent {

	@Autowired
    public void setProcessEngine(ProcessEngine processEngine) {
        super.setProcessEngine(processEngine);
    }
}
