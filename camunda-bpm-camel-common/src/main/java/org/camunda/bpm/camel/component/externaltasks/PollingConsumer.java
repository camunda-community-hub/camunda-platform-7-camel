package org.camunda.bpm.camel.component.externaltasks;

import static org.camunda.bpm.camel.component.CamundaBpmConstants.CAMUNDA_BPM_PROCESS_INSTANCE_ID;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.CAMUNDA_BPM_PROCESS_DEFINITION_ID;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.CAMUNDA_BPM_PROCESS_DEFINITION_KEY;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.impl.PollingConsumerSupport;
import org.camunda.bpm.camel.component.CamundaBpmEndpoint;
import org.camunda.bpm.camel.component.CamundaBpmExternalTaskEndpointImpl;
import org.camunda.bpm.engine.ExternalTaskService;
import org.camunda.bpm.engine.externaltask.ExternalTask;

public class PollingConsumer extends PollingConsumerSupport {

    private final CamundaBpmEndpoint camundaEndpoint;

    private final String topic;

    public PollingConsumer(final CamundaBpmEndpoint endpoint, final String topic) {

        super(endpoint);

        this.topic = topic;
        this.camundaEndpoint = endpoint;

    }

    private ExternalTaskService getExternalTaskService() {

        return camundaEndpoint.getProcessEngine().getExternalTaskService();

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

        final ExternalTaskService externalTaskService = getExternalTaskService();
System.err.println(externalTaskService);
        final ExternalTask task = externalTaskService.createExternalTaskQuery().topicName(
                topic).active().withRetriesLeft().orderById().asc().singleResult();
        if (task == null) {
            return null;
        }

        final Exchange result = getEndpoint().createExchange();
        result.setFromEndpoint(getEndpoint());
        result.setExchangeId(task.getWorkerId() + "/" + task.getId());
        result.setProperty(CAMUNDA_BPM_PROCESS_INSTANCE_ID, task.getProcessInstanceId());
        result.setProperty(CAMUNDA_BPM_PROCESS_DEFINITION_KEY, task.getProcessDefinitionKey());
        result.setProperty(CAMUNDA_BPM_PROCESS_DEFINITION_ID, task.getProcessDefinitionId());
        
        // result.setProperty(BatchConsumer.PROPERTY_PRIORITY, ???);

        final Message in = result.getIn();
        in.setHeader(CamundaBpmExternalTaskEndpointImpl.EXCHANGE_HEADER_TASK, task);

        return result;

    }

    @Override
    protected void doStart() throws Exception {

        // nothing to do

    }

    @Override
    protected void doStop() throws Exception {

        // nothing to do

    }

}
