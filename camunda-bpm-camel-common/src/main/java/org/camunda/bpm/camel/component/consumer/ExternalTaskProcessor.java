package org.camunda.bpm.camel.component.consumer;

import org.apache.camel.AsyncCallback;
import org.apache.camel.AsyncProcessor;
import org.apache.camel.Exchange;

public class ExternalTaskProcessor implements AsyncProcessor {

	@Override
	public void process(Exchange exchange) throws Exception {
		
		System.out.println("process");
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public boolean process(Exchange exchange, AsyncCallback callback) {
		System.out.println("asyncProcess");
		// TODO Auto-generated method stub
		return false;
	}
	
}
