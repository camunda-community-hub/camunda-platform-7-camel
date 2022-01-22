package org.camunda.bpm.camel.blueprint;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.camunda.bpm.camel.common.CamelServiceCommonImpl;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

public class CamelServiceImpl extends CamelServiceCommonImpl implements JavaDelegate { 
	
	public CamelServiceImpl(){
		setProcessEngine(OsgiHelper.getService(ProcessEngine.class));
		setCamelContext(new DefaultCamelContext());
	}

	public void setProcessEngine(ProcessEngine processEngine) {
		this.processEngine = processEngine;
	}
	
	public void setCamelContext(CamelContext camelContext) {
		this.camelContext = camelContext;
	}

	@Override
	public void execute(DelegateExecution execution) throws Exception {
		// Dummy implementation
	}

}
