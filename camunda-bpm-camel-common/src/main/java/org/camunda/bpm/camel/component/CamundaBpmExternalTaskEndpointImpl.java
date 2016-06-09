package org.camunda.bpm.camel.component;

import static org.camunda.bpm.camel.component.CamundaBpmConstants.RETRYTIMEOUT_PARAMETER;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.TOPIC_PARAMETER;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.MAXTASKSPERPOLL_PARAMETER;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.MAXTASKSPERPOLL_DEFAULT;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.LOCKDURATION_PARAMETER;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.LOCKDURATION_DEFAULT;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.COMPLETETASK_PARAMETER;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.COMPLETETASK_DEFAULT;

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

    private final String topic;
    
    private final boolean completeTask;

    private final int retryTimeout;
    
    private final int maxTasksPerPoll;
    
    private final long lockDuration;
    
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

        if (parameters.containsKey(MAXTASKSPERPOLL_PARAMETER)) {
            this.maxTasksPerPoll = Integer.parseInt((String) parameters.remove(MAXTASKSPERPOLL_PARAMETER));
        } else {
            this.maxTasksPerPoll = MAXTASKSPERPOLL_DEFAULT;
        }

        if (parameters.containsKey(COMPLETETASK_PARAMETER)) {
            this.completeTask = Boolean.parseBoolean((String) parameters.remove(COMPLETETASK_PARAMETER));
        } else {
            this.completeTask = COMPLETETASK_DEFAULT;
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
            		retryTimeout, lockDuration, topic, completeTask);
        } else {
            consumer = new BatchConsumer(this, processor, retryTimeout, lockDuration,
            		topic, completeTask);
        }
        configureConsumer(consumer);
        consumer.setMaxMessagesPerPoll(maxTasksPerPoll);

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
