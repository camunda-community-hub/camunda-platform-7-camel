package org.camunda.bpm.camel.component.externaltasks;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Message;
import org.apache.camel.impl.PollingConsumerSupport;

public class PollingConsumer extends PollingConsumerSupport {

	private final String topic;
	
	private boolean done = false;
	
	public PollingConsumer(final Endpoint endpoint, final String topic) {
		super(endpoint);
		this.topic = topic;
	}

	@Override
	public Exchange receive() {
		return receive(-1);
	}

	@Override
	public Exchange receiveNoWait() {
		return receive(0);
	}

	@Override
	public Exchange receive(long timeout) {
		
		if (done)
			return null;
		
		final Exchange result = getEndpoint().createExchange(ExchangePattern.InOut);
		result.setExchangeId(Long.toString(System.currentTimeMillis()));
		result.setFromEndpoint(getEndpoint());
		final Message in = result.getIn();
		in.setBody("JUHU");
		
		done = true;
		
		return result;
		
	}

	@Override
	protected void doStart() throws Exception {
		
		// TODO Auto-generated method stub
		System.err.println("doStart();");
		
	}

	@Override
	protected void doStop() throws Exception {
		
		// TODO Auto-generated method stub
		
	}
	
}
