package org.camunda.bpm.camel.component;

import static org.camunda.bpm.camel.component.CamundaBpmConstants.RETRYTIMEOUT_PARAMETER;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.TOPIC_PARAMETER;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.MAXMESSAGESPERPOLL_PARAMETER;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.MAXMESSAGESPERPOLL_DEFAULT;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.LOCKDURATION_PARAMETER;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.LOCKDURATION_DEFAULT;

import java.util.Map;

import org.apache.camel.Consumer;
import org.apache.camel.PollingConsumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultPollingEndpoint;
import org.camunda.bpm.camel.component.externaltasks.BatchConsumer;
import org.camunda.bpm.engine.ProcessEngine;

public class CamundaBpmExternalTaskEndpointImpl extends DefaultPollingEndpoint implements CamundaBpmEndpoint {

    public static final String EXCHANGE_HEADER_TASK = "camundaBpmExternalTask";

    private CamundaBpmComponent component;

    private String topic;

    private int retryTimeout;
    
    private int maxMessagesPerPoll;
    
    private long lockDuration;
    
    public CamundaBpmExternalTaskEndpointImpl(final String endpointUri, final CamundaBpmComponent component,
            final Map<String, Object> parameters) {

        super(endpointUri, component);

        this.component = component;

        if (parameters.containsKey(TOPIC_PARAMETER)) {
            this.topic = (String) parameters.remove(TOPIC_PARAMETER);
        } else {
            throw new IllegalArgumentException(
                    "You need to pass the '" + TOPIC_PARAMETER + "' parameter! Parameters received: " + parameters);
        }

        if (parameters.containsKey(RETRYTIMEOUT_PARAMETER)) {
            this.retryTimeout = Integer.parseInt((String) parameters.remove(RETRYTIMEOUT_PARAMETER));
        } else {
            this.retryTimeout = 0;
        }

        if (parameters.containsKey(MAXMESSAGESPERPOLL_PARAMETER)) {
            this.maxMessagesPerPoll = Integer.parseInt((String) parameters.remove(MAXMESSAGESPERPOLL_PARAMETER));
        } else {
            this.maxMessagesPerPoll = MAXMESSAGESPERPOLL_DEFAULT;
        }

        if (parameters.containsKey(LOCKDURATION_PARAMETER)) {
            this.lockDuration = Integer.parseInt((String) parameters.remove(LOCKDURATION_PARAMETER));
        } else {
            this.lockDuration = LOCKDURATION_DEFAULT;
        }
        
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {

        final BatchConsumer consumer;
        if (getScheduledExecutorService() != null) {
            consumer = new BatchConsumer(this, processor, getScheduledExecutorService(), 
            		retryTimeout, lockDuration, topic);
        } else {
            consumer = new BatchConsumer(this, processor, retryTimeout, lockDuration, topic);
        }
        configureConsumer(consumer);
        consumer.setMaxMessagesPerPoll(maxMessagesPerPoll);

        return consumer;

    }

    @Override
    public PollingConsumer createPollingConsumer() throws Exception {
    	
    	return null;
        //return new org.camunda.bpm.camel.component.externaltasks.PollingConsumer(this, topic);

    }

    @Override
    public Producer createProducer() throws Exception {

        return null;

    }

    @Override
    public boolean isSingleton() {

        return true;

    }

    @Override
    public ProcessEngine getProcessEngine() {

        return component.getProcessEngine();

    }

}
