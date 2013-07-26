package org.camunda.bpm.camel.cdi;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.camunda.bpm.camel.common.CamundaBpmProducer;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(Arquillian.class)
public class SendToCamelIT extends BaseArquillianIntegrationTest {

  private static String PROCESS_DEFINITION_KEY = "sendToCamelProcess";

  @Deployment
  public static WebArchive createDeployment() {
    return prepareTestDeployment(PROCESS_DEFINITION_KEY, "process/SendToCamel.bpmn20.xml", SendToCamelRoute.class);
  }

  MockEndpoint mockEndpoint;

  @Before
  public void setUp() {
//    mockEndpoint = (MockEndpoint) camelContext.getEndpoint("mock:endpoint");
    mockEndpoint.reset();
  }

  @Test
  public void doTest() throws InterruptedException {
    Map<String, Object> processVariables = new HashMap<String, Object>();
    processVariables.put("var1", "foo");
    processVariables.put("var2", "bar");
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("sendToCamelProcess", processVariables);

    // Verify that a process instance was executed and there are no instances executing now
    assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionKey("sendToCamelProcess").count()).isEqualTo(1);
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(0);

    // Assert that the camunda BPM process instance ID has been added as a property to the message
    assertThat(mockEndpoint.assertExchangeReceived(0).getProperty(CamundaBpmProducer.PROCESS_ID_PROPERTY)).isEqualTo(processInstance.getId());

    // FIXME: check that var2 is also present as a property!

//    runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY);
//    Task task = taskService.createTaskQuery().processDefinitionKey(PROCESS_DEFINITION_KEY).singleResult();
//    assertThat(task).isNotNull();
//    assertThat("My Task").isEqualTo(task.getName());
//
//    taskService.complete(task.getId());
//    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey(PROCESS_DEFINITION_KEY).count()).isEqualTo(0);
  }
}