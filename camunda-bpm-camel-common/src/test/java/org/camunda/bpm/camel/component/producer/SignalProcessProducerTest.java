package org.camunda.bpm.camel.component.producer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.ACTIVITY_ID_PARAMETER;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.PROCESS_DEFINITION_KEY_PARAMETER;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.camundaBpmUri;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.camel.Endpoint;
import org.apache.camel.Producer;
import org.camunda.bpm.camel.BaseCamelTest;
import org.camunda.bpm.camel.component.CamundaBpmEndpoint;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.junit.Test;

public class SignalProcessProducerTest extends BaseCamelTest {

    @Test
    public void getSignalProcessProducerFromUri() throws Exception {
        CamundaBpmEndpoint endpoint = (CamundaBpmEndpoint) camelContext.getEndpoint(
                camundaBpmUri("signal?" + PROCESS_DEFINITION_KEY_PARAMETER + "=" + "aProcessDefinitionKey" + "&"
                        + ACTIVITY_ID_PARAMETER + "=" + "anActivityId"));
        Producer producer = endpoint.createProducer();
        assertThat(producer).isInstanceOf(MessageProducer.class);
    }

    // No longer valid - The process definition key may be past at execution of
    // the route
    // @Test(expected = IllegalArgumentException.class)
    // public void noProcessDefinitionKeyParameterShouldThrowException() throws
    // Exception {
    // Endpoint endpoint = camelContext.getEndpoint(camundaBpmUri("signal"));
    // endpoint.createProducer();
    // }

    @Test(expected = IllegalArgumentException.class)
    public void noActivityIdParameterShouldThrowException() throws Exception {
        Endpoint endpoint = camelContext.getEndpoint(
                camundaBpmUri("signal?" + PROCESS_DEFINITION_KEY_PARAMETER + "=" + "aProcessDefinitionKey"));
        endpoint.createProducer();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void signalShouldBeCalled() throws Exception {
        ProcessInstance processInstance = mock(ProcessInstance.class);
        when(processInstance.getProcessInstanceId()).thenReturn("theProcessInstanceId");
        when(processInstance.getProcessDefinitionId()).thenReturn("theProcessDefinitionId");
        when(runtimeService.startProcessInstanceByKey(eq("aProcessDefinitionKey"), anyMap())).thenReturn(
                processInstance);

        CamundaBpmEndpoint endpoint = (CamundaBpmEndpoint) camelContext.getEndpoint(
                camundaBpmUri("signal?" + PROCESS_DEFINITION_KEY_PARAMETER + "=" + "aProcessDefinitionKey" + "&"
                        + ACTIVITY_ID_PARAMETER + "=" + "anActivityId"));
        Producer producer = endpoint.createProducer();
        assertThat(producer).isInstanceOf(MessageProducer.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void signalWithBusinessKeyShouldBeCalled() throws Exception {
        ProcessInstance processInstance = mock(ProcessInstance.class);
        when(processInstance.getProcessInstanceId()).thenReturn("theProcessInstanceId");
        when(processInstance.getProcessDefinitionId()).thenReturn("theProcessDefinitionId");
        when(runtimeService.startProcessInstanceByKey(eq("aProcessDefinitionKey"), anyMap())).thenReturn(
                processInstance);

        CamundaBpmEndpoint endpoint = (CamundaBpmEndpoint) camelContext.getEndpoint(
                camundaBpmUri("signal?" + PROCESS_DEFINITION_KEY_PARAMETER + "=" + "aProcessDefinitionKey" + "&"
                        + ACTIVITY_ID_PARAMETER + "=" + "anActivityId"));
        Producer producer = endpoint.createProducer();
        assertThat(producer).isInstanceOf(MessageProducer.class);
    }

}
