package org.camunda.bpm.camel.component.producer;

import org.apache.camel.Exchange;
import org.apache.camel.Producer;
import org.apache.camel.support.DefaultExchange;
import org.camunda.bpm.camel.BaseCamelTest;
import org.camunda.bpm.camel.component.CamundaBpmEndpoint;
import org.camunda.bpm.engine.runtime.ProcessInstanceWithVariables;
import org.camunda.bpm.engine.runtime.ProcessInstantiationBuilder;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.*;
import static org.mockito.Mockito.*;

public class StartProcessProducerTest extends BaseCamelTest {

    @Test
    public void getStartProcessProducerFromUri() throws Exception {
        CamundaBpmEndpoint endpoint = (CamundaBpmEndpoint) camelContext.getEndpoint(
                camundaBpmUri("start?" + PROCESS_DEFINITION_KEY_PARAMETER + "=" + "aProcessDefinitionKey"));
        Producer producer = endpoint.createProducer();
        assertThat(producer).isInstanceOf(StartProcessProducer.class);
    }

    // No longer valid - The process definition key may be past at execution of
    // the route
    // @Test(expected = IllegalArgumentException.class)
    // public void noProcessDefinitionKeyParameterShouldThrowException() throws
    // Exception {
    // Endpoint endpoint = camelContext.getEndpoint(camundaBpmUri("start"));
    // endpoint.createProducer(); // This triggers the exception
    // }

    @SuppressWarnings("unchecked")
    @Test
    public void createProcessInstanceByKeyShouldBeCalled() throws Exception {
        ProcessInstanceWithVariables processInstance = mock(ProcessInstanceWithVariables.class);
        ProcessInstantiationBuilder processInstantiationBuilder = mock(ProcessInstantiationBuilder.class);
        when(processInstance.getProcessInstanceId()).thenReturn("theProcessInstanceId");
        when(processInstance.getProcessDefinitionId()).thenReturn("theProcessDefinitionId");
        when(runtimeService.createProcessInstanceByKey(eq("aProcessDefinitionKey"))).thenReturn(
                processInstantiationBuilder);
        when(processInstantiationBuilder.setVariables(anyMap())).thenReturn(processInstantiationBuilder);
        when(processInstantiationBuilder.executeWithVariablesInReturn()).thenReturn(processInstance);

        CamundaBpmEndpoint endpoint = (CamundaBpmEndpoint) camelContext.getEndpoint(
                camundaBpmUri("start?" + PROCESS_DEFINITION_KEY_PARAMETER + "=" + "aProcessDefinitionKey"));
        StartProcessProducer producer = (StartProcessProducer) endpoint.createProducer();
        Exchange exchange = new DefaultExchange(camelContext);
        producer.process(exchange);

        verify(runtimeService, times(1)).createProcessInstanceByKey(eq("aProcessDefinitionKey"));
        assertThat(exchange.getProperty(EXCHANGE_HEADER_PROCESS_DEFINITION_ID)).isEqualTo("theProcessDefinitionId");
        assertThat(exchange.getProperty(EXCHANGE_HEADER_PROCESS_INSTANCE_ID)).isEqualTo("theProcessInstanceId");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void createProcessInstanceByKeyWithBusinessKeyShouldBeCalled() throws Exception {
        ProcessInstanceWithVariables processInstance = mock(ProcessInstanceWithVariables.class);
        ProcessInstantiationBuilder processInstantiationBuilder = mock(ProcessInstantiationBuilder.class);
        when(processInstance.getProcessInstanceId()).thenReturn("theProcessInstanceId");
        when(processInstance.getProcessDefinitionId()).thenReturn("theProcessDefinitionId");
        when(processInstance.getBusinessKey()).thenReturn("aBusinessKey");
        when(runtimeService.createProcessInstanceByKey(eq("aProcessDefinitionKey")))
                .thenReturn(processInstantiationBuilder);
        when(processInstantiationBuilder.setVariables(anyMap())).thenReturn(processInstantiationBuilder);
        when(processInstantiationBuilder.businessKey(anyString())).thenReturn(processInstantiationBuilder);
        when(processInstantiationBuilder.executeWithVariablesInReturn()).thenReturn(processInstance);

        CamundaBpmEndpoint endpoint = (CamundaBpmEndpoint) camelContext.getEndpoint(
                camundaBpmUri("start?" + PROCESS_DEFINITION_KEY_PARAMETER + "=" + "aProcessDefinitionKey"));
        StartProcessProducer producer = (StartProcessProducer) endpoint.createProducer();
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.setProperty(EXCHANGE_HEADER_BUSINESS_KEY, "aBusinessKey");
        producer.process(exchange);

        verify(runtimeService, times(1)).createProcessInstanceByKey(eq("aProcessDefinitionKey"));
        verify(processInstantiationBuilder, times(1)).businessKey(eq("aBusinessKey"));
        assertThat(exchange.getProperty(EXCHANGE_HEADER_PROCESS_DEFINITION_ID)).isEqualTo("theProcessDefinitionId");
        assertThat(exchange.getProperty(EXCHANGE_HEADER_PROCESS_INSTANCE_ID)).isEqualTo("theProcessInstanceId");
        assertThat(exchange.getProperty(EXCHANGE_HEADER_BUSINESS_KEY)).isEqualTo("aBusinessKey");
    }
}
