package org.camunda.bpm.camel.component;

import static org.camunda.bpm.camel.component.CamundaBpmConstants.TOPIC_PARAMETER;

import java.util.Map;

import org.apache.camel.Component;
import org.apache.camel.PollingConsumer;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultPollingEndpoint;

public class CamundaBpmExternalTaskEndpoint extends DefaultPollingEndpoint {

    private String topic;

    public CamundaBpmExternalTaskEndpoint(String endpointUri, Component component, Map<String, Object> parameters) {

        super(endpointUri, component);

        if (parameters.containsKey(TOPIC_PARAMETER)) {
            this.topic = (String) parameters.remove(TOPIC_PARAMETER);
        } else {
            throw new IllegalArgumentException(
                    "You need to pass the '" + TOPIC_PARAMETER + "' parameter! Parameters received: " + parameters);
        }

    }

    @Override
    public PollingConsumer createPollingConsumer() throws Exception {

        return new org.camunda.bpm.camel.component.externaltasks.PollingConsumer(this, topic);

    }

    @Override
    public Producer createProducer() throws Exception {

        return null;

    }

    @Override
    public boolean isSingleton() {
        return true;
    }

}
