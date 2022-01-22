package org.camunda.bpm.camel.blueprint.component;

import org.camunda.bpm.camel.blueprint.OsgiHelper;
import org.camunda.bpm.camel.component.CamundaBpmComponent;
import org.camunda.bpm.engine.ProcessEngine;

public class CamundaBpmBlueprintComponent extends CamundaBpmComponent {

	public CamundaBpmBlueprintComponent() {
		super(null);
	}

	public CamundaBpmBlueprintComponent(ProcessEngine processEngine) {
		super(processEngine);
	}

	public ProcessEngine getProcessEngine() {
		if(this.processEngine == null){
			setProcessEngine(OsgiHelper.getService(ProcessEngine.class));
		}
		return this.processEngine;
	}

}
