package org.camunda.bpm.camel.component.producer;

import static org.camunda.bpm.camel.component.CamundaBpmConstants.ACTIVITY_ID_PARAMETER;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.camundaBpmUri;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.camel.Producer;
import org.camunda.bpm.camel.BaseCamelTest;
import org.camunda.bpm.camel.component.CamundaBpmEndpoint;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.junit.Test;

public class MessageProducerTest extends BaseCamelTest {

  @Test
  public void getSignalProcessProducerFromUri() throws Exception {
    CamundaBpmEndpoint endpoint = (CamundaBpmEndpoint) camelContext.getEndpoint(camundaBpmUri("message?" +
        ACTIVITY_ID_PARAMETER + "=" + "anActivityId" ));
    Producer producer = endpoint.createProducer();
    assertThat(producer).isInstanceOf(MessageProducer.class);
  }

  @Test
  public void messageIsDeliveredCalled() throws Exception {
    ProcessInstance processInstance = mock(ProcessInstance.class);
    when(processInstance.getProcessInstanceId()).thenReturn("theProcessInstanceId");
    when(processInstance.getProcessDefinitionId()).thenReturn("theProcessDefinitionId");
    when(runtimeService.startProcessInstanceByKey(eq("aProcessDefinitionKey"), anyMap())).thenReturn(processInstance);

    CamundaBpmEndpoint endpoint = (CamundaBpmEndpoint) camelContext.getEndpoint(camundaBpmUri("message?" +
        ACTIVITY_ID_PARAMETER + "=" + "anActivityId" ));
    Producer producer = endpoint.createProducer();
    assertThat(producer).isInstanceOf(MessageProducer.class);
  }


}
