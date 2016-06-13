package org.camunda.bpm.camel.component;

import static org.camunda.bpm.camel.component.CamundaBpmConstants.COMPLETETASK_DEFAULT;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.COMPLETETASK_PARAMETER;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.LOCKDURATION_DEFAULT;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.LOCKDURATION_PARAMETER;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.MAXTASKSPERPOLL_DEFAULT;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.MAXTASKSPERPOLL_PARAMETER;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.RETRIES_PARAMETER;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.RETRYTIMEOUTS_PARAMETER;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.RETRYTIMEOUT_PARAMETER;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.RETRYTIMEOUT_DEFAULT;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.TOPIC_PARAMETER;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.VARIABLESTOFETCH_PARAMETER;

import java.util.List;
import java.util.Map;

import org.apache.camel.Consumer;
import org.apache.camel.PollingConsumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.converter.TimePatternConverter;
import org.apache.camel.impl.DefaultPollingEndpoint;
import org.camunda.bpm.camel.component.externaltasks.BatchConsumer;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.model.xml.impl.util.StringUtil;

public class CamundaBpmExternalTaskEndpointImpl extends DefaultPollingEndpoint implements CamundaBpmEndpoint {

    public static final String EXCHANGE_HEADER_TASK = "CamundaBpmExternalTask";

    private CamundaBpmComponent component;

    // parameters
    private final String topic;
    private final boolean completeTask;
    private final int retries;
    private final long retryTimeout;
    private final long[] retryTimeouts;
    private final int maxTasksPerPoll;
    private final long lockDuration;
    private final List<String> variablesToFetch;

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

        if (parameters.containsKey(RETRIES_PARAMETER)) {
            this.retries = Integer.parseInt((String) parameters.remove(RETRIES_PARAMETER));
        } else {
            this.retries = 0;
        }
        
        if (parameters.containsKey(RETRYTIMEOUT_PARAMETER)) {
            this.retryTimeout = TimePatternConverter.toMilliSeconds((String) parameters.remove(RETRYTIMEOUT_PARAMETER));
        } else {
            this.retryTimeout = RETRYTIMEOUT_DEFAULT;
        }

        if (parameters.containsKey(RETRYTIMEOUTS_PARAMETER)) {
            final String retryTimeoutsString = (String) parameters.remove(RETRYTIMEOUTS_PARAMETER);
            final String[] retryTimeoutsStrings = retryTimeoutsString.split(",");
            retryTimeouts = new long[retryTimeoutsStrings.length];
            for (int i = 0; i < retryTimeoutsStrings.length; ++i) {
            	retryTimeouts[i] = TimePatternConverter.toMilliSeconds(retryTimeoutsStrings[i]);
            }
        } else {
        	retryTimeouts = null;
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
            this.lockDuration = TimePatternConverter.toMilliSeconds((String) parameters.remove(LOCKDURATION_PARAMETER));
        } else {
            this.lockDuration = LOCKDURATION_DEFAULT;
        }

        if (parameters.containsKey(VARIABLESTOFETCH_PARAMETER)) {
            variablesToFetch = StringUtil.splitListBySeparator((String) parameters.get(VARIABLESTOFETCH_PARAMETER),
                    ",");
        } else {
            variablesToFetch = null;
        }

    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {

        final BatchConsumer consumer;
        if (getScheduledExecutorService() != null) {
            consumer = new BatchConsumer(this,
                    processor,
                    getScheduledExecutorService(),
                    retries,
                    retryTimeout,
                    retryTimeouts,
                    lockDuration,
                    topic,
                    completeTask,
                    variablesToFetch);
        } else {
            consumer = new BatchConsumer(this,
                    processor,
                    retries,
                    retryTimeout,
                    retryTimeouts,
                    lockDuration,
                    topic,
                    completeTask,
                    variablesToFetch);
        }
        configureConsumer(consumer);
        consumer.setMaxMessagesPerPoll(maxTasksPerPoll);

        return consumer;

    }

    @Override
    public PollingConsumer createPollingConsumer() throws Exception {

        return null;
        // return new
        // org.camunda.bpm.camel.component.externaltasks.PollingConsumer(this,
        // topic);

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
