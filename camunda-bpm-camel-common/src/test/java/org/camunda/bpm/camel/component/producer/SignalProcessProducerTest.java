package org.camunda.bpm.camel.component.producer;

import org.apache.camel.Endpoint;
import org.apache.camel.Producer;
import org.camunda.bpm.camel.BaseCamelTest;
import org.camunda.bpm.camel.component.CamundaBpmEndpoint;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.junit.Test;

import static org.camunda.bpm.camel.component.CamundaBpmConstants.*;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SignalProcessProducerTest extends BaseCamelTest {

  @Test
  public void getSignalProcessProducerFromUri() throws Exception {
    CamundaBpmEndpoint endpoint = (CamundaBpmEndpoint) camelContext.getEndpoint(camundaBpmUri("signal?" +
                                    PROCESS_DEFINITION_KEY_PARAMETER + "=" + "aProcessDefinitionKey" + "&" +
                                    ACTIVITY_ID_PARAMETER + "=" + "anActivityId" ));
    Producer producer = endpoint.createProducer();
    assertThat(producer).isInstanceOf(MessageProducer.class);
  }

  @Test(expected = IllegalArgumentException.class)
  public void noProcessDefinitionKeyParameterShouldThrowException() throws Exception {
    Endpoint endpoint = camelContext.getEndpoint(camundaBpmUri("signal"));
    endpoint.createProducer();
  }

  @Test(expected = IllegalArgumentException.class)
  public void noActivityIdParameterShouldThrowException() throws Exception {
    Endpoint endpoint = camelContext.getEndpoint(camundaBpmUri("signal"  +
                                                 PROCESS_DEFINITION_KEY_PARAMETER + "=" + "aProcessDefinitionKey"));
    endpoint.createProducer();
  }

  @Test
  public void signalShouldBeCalled() throws Exception {
    ProcessInstance processInstance = mock(ProcessInstance.class);
    when(processInstance.getProcessInstanceId()).thenReturn("theProcessInstanceId");
    when(processInstance.getProcessDefinitionId()).thenReturn("theProcessDefinitionId");
    when(runtimeService.startProcessInstanceByKey(eq("aProcessDefinitionKey"), anyMap())).thenReturn(processInstance);

    CamundaBpmEndpoint endpoint = (CamundaBpmEndpoint) camelContext.getEndpoint(camundaBpmUri("signal?" +
      PROCESS_DEFINITION_KEY_PARAMETER + "=" + "aProcessDefinitionKey" + "&" +
      ACTIVITY_ID_PARAMETER + "=" + "anActivityId" ));
    Producer producer = endpoint.createProducer();
    assertThat(producer).isInstanceOf(MessageProducer.class);
  }

  @Test
  public void signalWithBusinessKeyShouldBeCalled() throws Exception {
    ProcessInstance processInstance = mock(ProcessInstance.class);
    when(processInstance.getProcessInstanceId()).thenReturn("theProcessInstanceId");
    when(processInstance.getProcessDefinitionId()).thenReturn("theProcessDefinitionId");
    when(runtimeService.startProcessInstanceByKey(eq("aProcessDefinitionKey"), anyMap())).thenReturn(processInstance);

    CamundaBpmEndpoint endpoint = (CamundaBpmEndpoint) camelContext.getEndpoint(camundaBpmUri("signal?" +
      PROCESS_DEFINITION_KEY_PARAMETER + "=" + "aProcessDefinitionKey" + "&" +
      ACTIVITY_ID_PARAMETER + "=" + "anActivityId" ));
    Producer producer = endpoint.createProducer();
    assertThat(producer).isInstanceOf(MessageProducer.class);
  }

}
