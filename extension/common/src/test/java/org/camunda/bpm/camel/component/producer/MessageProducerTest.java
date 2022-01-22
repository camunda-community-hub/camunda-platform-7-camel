package org.camunda.bpm.camel.component.producer;

import static org.camunda.bpm.camel.component.CamundaBpmConstants.ACTIVITY_ID_PARAMETER;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.EXCHANGE_HEADER_BUSINESS_KEY;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.EXCHANGE_HEADER_CORRELATION_KEY;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.EXCHANGE_HEADER_CORRELATION_KEY_TYPE;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.EXCHANGE_HEADER_PROCESS_INSTANCE_ID;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.CORRELATION_KEY_NAME_PARAMETER;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.MESSAGE_NAME_PARAMETER;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.COPY_MESSAGE_BODY_AS_PROCESS_VARIABLE_PARAMETER;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.camundaBpmUri;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.ExtendedExchange;
import org.apache.camel.Message;
import org.apache.camel.Producer;
import org.camunda.bpm.camel.BaseCamelTest;
import org.camunda.bpm.camel.component.CamundaBpmEndpoint;
import org.camunda.bpm.engine.runtime.Execution;
import org.camunda.bpm.engine.runtime.ExecutionQuery;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.runtime.ProcessInstanceQuery;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class MessageProducerTest extends BaseCamelTest {

    @Test
    public void getSignalProcessProducerFromUri() throws Exception {
        CamundaBpmEndpoint endpoint = (CamundaBpmEndpoint) camelContext.getEndpoint(
                camundaBpmUri("message?" + ACTIVITY_ID_PARAMETER + "=" + "anActivityId"));
        Producer producer = endpoint.createProducer();
        assertThat(producer).isInstanceOf(MessageProducer.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void messageIsDeliveredCalled() throws Exception {
        ProcessInstance processInstance = mock(ProcessInstance.class);
        when(processInstance.getProcessInstanceId()).thenReturn("theProcessInstanceId");
        when(processInstance.getProcessDefinitionId()).thenReturn("theProcessDefinitionId");
        when(runtimeService.startProcessInstanceByKey(eq("aProcessDefinitionKey"), anyMap())).thenReturn(
                processInstance);

        CamundaBpmEndpoint endpoint = (CamundaBpmEndpoint) camelContext.getEndpoint(
                camundaBpmUri("message?" + ACTIVITY_ID_PARAMETER + "=" + "anActivityId"));
        Producer producer = endpoint.createProducer();
        assertThat(producer).isInstanceOf(MessageProducer.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void signalCalled() throws Exception {
        Exchange exchange = mock(Exchange.class);
        Message message = mock(Message.class);
        ExecutionQuery query = mock(ExecutionQuery.class);
        Execution execution = mock(Execution.class);

        when(exchange.getIn()).thenReturn(message);
        when(exchange.getProperty(eq(EXCHANGE_HEADER_PROCESS_INSTANCE_ID), eq(String.class))).thenReturn(
                "theProcessInstanceId");
        when(runtimeService.createExecutionQuery()).thenReturn(query);
        when(query.processInstanceId(anyString())).thenReturn(query);
        when(query.activityId(anyString())).thenReturn(query);
        when(query.singleResult()).thenReturn(execution);
        when(execution.getId()).thenReturn("1234");

        CamundaBpmEndpoint endpoint = (CamundaBpmEndpoint) camelContext.getEndpoint(
                camundaBpmUri("message?" + ACTIVITY_ID_PARAMETER + "=" + "anActivityId"));
        Producer producer = endpoint.createProducer();

        producer.process(exchange);

        verify(runtimeService).signal(anyString(), anyMap());
    }

    @Test
    public void signalTransformBusinesskey() throws Exception {
        Exchange exchange = mock(ExtendedExchange.class);
        Message message = mock(Message.class);
        ExecutionQuery query = mock(ExecutionQuery.class);
        Execution execution = mock(Execution.class);
        ProcessInstanceQuery piQuery = mock(ProcessInstanceQuery.class);
        ProcessInstance processInstance = mock(ProcessInstance.class);

        when(exchange.getIn()).thenReturn(message);
        when(exchange.getProperty(eq(EXCHANGE_HEADER_BUSINESS_KEY), eq(String.class))).thenReturn("theBusinessKey");

        when(runtimeService.createProcessInstanceQuery()).thenReturn(piQuery);
        when(runtimeService.createExecutionQuery()).thenReturn(query);
        when(piQuery.processInstanceBusinessKey(anyString())).thenReturn(piQuery);
        when(piQuery.singleResult()).thenReturn(processInstance);
        when(processInstance.getId()).thenReturn("theProcessInstanceId");

        when(query.processInstanceId(anyString())).thenReturn(query);
        when(query.activityId(anyString())).thenReturn(query);
        when(query.singleResult()).thenReturn(execution);

        CamundaBpmEndpoint endpoint = (CamundaBpmEndpoint) camelContext.getEndpoint(
                camundaBpmUri("message?" + ACTIVITY_ID_PARAMETER + "=" + "anActivityId"));
        Producer producer = endpoint.createProducer();

        producer.process(exchange);

        verify(piQuery).processInstanceBusinessKey("theBusinessKey");
        verify(query).processInstanceId("theProcessInstanceId");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void messageProcessInstanceId() throws Exception {
        Exchange exchange = mock(Exchange.class);
        Message message = mock(Message.class);
        ExecutionQuery query = mock(ExecutionQuery.class);
        Execution execution = mock(Execution.class);

        when(exchange.getIn()).thenReturn(message);
        when(exchange.getProperty(eq(EXCHANGE_HEADER_PROCESS_INSTANCE_ID), eq(String.class))).thenReturn(
                "theProcessInstanceId");
        when(runtimeService.createExecutionQuery()).thenReturn(query);
        when(query.processInstanceId(anyString())).thenReturn(query);
        when(query.messageEventSubscriptionName(anyString())).thenReturn(query);
        when(query.singleResult()).thenReturn(execution);
        when(execution.getId()).thenReturn("theExecutionId");

        CamundaBpmEndpoint endpoint = (CamundaBpmEndpoint) camelContext.getEndpoint(
                camundaBpmUri("message?" + MESSAGE_NAME_PARAMETER + "=" + "aMessageName"));
        Producer producer = endpoint.createProducer();

        producer.process(exchange);

        verify(query).processInstanceId("theProcessInstanceId");
        verify(query).messageEventSubscriptionName("aMessageName");

        verify(runtimeService).messageEventReceived(eq("aMessageName"), eq("theExecutionId"), anyMap());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void messageBusinessKey() throws Exception {
        Exchange exchange = mock(Exchange.class);
        Message message = mock(Message.class);

        when(exchange.getIn()).thenReturn(message);
        when(exchange.getProperty(eq(EXCHANGE_HEADER_BUSINESS_KEY), eq(String.class))).thenReturn("theBusinessKey");

        CamundaBpmEndpoint endpoint = (CamundaBpmEndpoint) camelContext.getEndpoint(
                camundaBpmUri("message?" + MESSAGE_NAME_PARAMETER + "=" + "aMessageName"));
        Producer producer = endpoint.createProducer();

        producer.process(exchange);

        @SuppressWarnings("rawtypes")
        Class<Map<String, Object>> mapClass = (Class<Map<String, Object>>) (Class) Map.class;
        ArgumentCaptor<Map<String, Object>> correlationCaptor = ArgumentCaptor.forClass(mapClass);

        verify(runtimeService).correlateMessage(eq("aMessageName"),
                eq("theBusinessKey"),
                correlationCaptor.capture(),
                anyMap());

        assertThat(correlationCaptor.getValue().size()).isEqualTo(0);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void messageBusinessKeyCorrelationKey() throws Exception {
        Exchange exchange = mock(Exchange.class);
        Message message = mock(Message.class);

        final String BODY = "body";
        when(message.getBody()).thenReturn(BODY);
        when(exchange.getIn()).thenReturn(message);
        when(exchange.getProperty(eq(EXCHANGE_HEADER_BUSINESS_KEY), eq(String.class))).thenReturn("theBusinessKey");
        when(exchange.getProperty(eq(EXCHANGE_HEADER_CORRELATION_KEY), eq(String.class))).thenReturn("theCorrelationKey");

        CamundaBpmEndpoint endpoint = (CamundaBpmEndpoint) camelContext.getEndpoint(camundaBpmUri(
                "message?" + MESSAGE_NAME_PARAMETER + "=" + "aMessageName" + "&" + CORRELATION_KEY_NAME_PARAMETER + "="
                        + "aCorrelationKeyName" + "&" + COPY_MESSAGE_BODY_AS_PROCESS_VARIABLE_PARAMETER + "=test"));

        Producer producer = endpoint.createProducer();

        producer.process(exchange);

        @SuppressWarnings("rawtypes")
        Class<Map<String, Object>> mapClass = (Class<Map<String, Object>>) (Class) Map.class;
        ArgumentCaptor<Map<String, Object>> correlationCaptor = ArgumentCaptor.forClass(mapClass);
        ArgumentCaptor<Map<String, Object>> variablesCaptor = ArgumentCaptor.forClass(mapClass);

        verify(runtimeService).correlateMessage(eq("aMessageName"),
                eq("theBusinessKey"),
                correlationCaptor.capture(),
                variablesCaptor.capture());

        assertThat(correlationCaptor.getValue().size()).isEqualTo(1);
        assertTrue(correlationCaptor.getValue().keySet().contains("aCorrelationKeyName"));
        assertTrue(correlationCaptor.getValue().values().contains("theCorrelationKey"));
        assertThat(variablesCaptor.getValue().size()).isEqualTo(1);
        assertTrue(variablesCaptor.getValue().containsKey("test"));
        assertTrue(variablesCaptor.getValue().containsValue(BODY));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void messageBusinessKeyCorrelationKeyType() throws Exception {
        Exchange exchange = mock(Exchange.class);
        Message message = mock(Message.class);

        when(exchange.getIn()).thenReturn(message);
        when(exchange.getProperty(eq(EXCHANGE_HEADER_BUSINESS_KEY), eq(String.class))).thenReturn("theBusinessKey");

        when(exchange.getProperty(eq(EXCHANGE_HEADER_CORRELATION_KEY), eq(java.lang.Integer.class))).thenReturn(15);

        when(exchange.getProperty(eq(EXCHANGE_HEADER_CORRELATION_KEY_TYPE), eq(String.class))).thenReturn(
                "java.lang.Integer");

        CamundaBpmEndpoint endpoint = (CamundaBpmEndpoint) camelContext.getEndpoint(camundaBpmUri(
                "message?" + MESSAGE_NAME_PARAMETER + "=" + "aMessageName" + "&" + CORRELATION_KEY_NAME_PARAMETER + "="
                        + "aCorrelationKeyName" + "&" + EXCHANGE_HEADER_CORRELATION_KEY_TYPE + "=java.lang.Integer"));

        Producer producer = endpoint.createProducer();

        producer.process(exchange);

        @SuppressWarnings("rawtypes")
        Class<Map<String, Object>> mapClass = (Class<Map<String, Object>>) (Class) Map.class;
        ArgumentCaptor<Map<String, Object>> correlationCaptor = ArgumentCaptor.forClass(mapClass);

        verify(runtimeService).correlateMessage(eq("aMessageName"),
                eq("theBusinessKey"),
                correlationCaptor.capture(),
                anyMap());

        assertThat(correlationCaptor.getValue().size()).isEqualTo(1);
        assertTrue(correlationCaptor.getValue().keySet().contains("aCorrelationKeyName"));
        assertTrue(correlationCaptor.getValue().values().contains(15));

    }

    @SuppressWarnings("unchecked")
    @Test
    public void messageNoKey() throws Exception {
        Exchange exchange = mock(Exchange.class);
        Message message = mock(Message.class);

        when(exchange.getIn()).thenReturn(message);

        CamundaBpmEndpoint endpoint = (CamundaBpmEndpoint) camelContext.getEndpoint(
                camundaBpmUri("message?" + MESSAGE_NAME_PARAMETER + "=" + "aMessageName"));
        Producer producer = endpoint.createProducer();

        producer.process(exchange);

        @SuppressWarnings("rawtypes")
        Class<Map<String, Object>> mapClass = (Class<Map<String, Object>>) (Class) Map.class;
        ArgumentCaptor<Map<String, Object>> correlationCaptor = ArgumentCaptor.forClass(mapClass);
        verify(runtimeService).correlateMessage(eq("aMessageName"),
                correlationCaptor.capture(),
                anyMapOf(String.class, Object.class));

        assertThat(correlationCaptor.getValue().size()).isEqualTo(0);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void messageCorrelationKey() throws Exception {
        Exchange exchange = mock(Exchange.class);
        Message message = mock(Message.class);

        when(exchange.getIn()).thenReturn(message);
        when(exchange.getProperty(eq(EXCHANGE_HEADER_CORRELATION_KEY), eq(String.class))).thenReturn("theCorrelationKey");

        CamundaBpmEndpoint endpoint = (CamundaBpmEndpoint) camelContext.getEndpoint(
                camundaBpmUri("message?" + MESSAGE_NAME_PARAMETER + "=" + "aMessageName" + "&"
                        + CORRELATION_KEY_NAME_PARAMETER + "=" + "aCorrelationKeyName"));
        Producer producer = endpoint.createProducer();

        producer.process(exchange);

        @SuppressWarnings("rawtypes")
        Class<Map<String, Object>> mapClass = (Class<Map<String, Object>>) (Class) Map.class;
        ArgumentCaptor<Map<String, Object>> correlationCaptor = ArgumentCaptor.forClass(mapClass);
        verify(runtimeService).correlateMessage(eq("aMessageName"),
                correlationCaptor.capture(),
                anyMapOf(String.class, Object.class));

        assertThat(correlationCaptor.getValue().size()).isEqualTo(1);
        assertTrue(correlationCaptor.getValue().keySet().contains("aCorrelationKeyName"));
        assertTrue(correlationCaptor.getValue().values().contains("theCorrelationKey"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailWithoutMessageActivityId() throws Exception {
        CamundaBpmEndpoint endpoint = (CamundaBpmEndpoint) camelContext.getEndpoint(camundaBpmUri("message"));
        endpoint.createProducer();
    }
}