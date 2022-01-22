package org.camunda.bpm.camel.cdi;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.cdi.Uri;
import org.apache.camel.component.mock.MockEndpoint;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.*;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(Arquillian.class)
public class ReceiveFromCamelIT extends BaseArquillianIntegrationTest {

  private static String PROCESS_DEFINITION_KEY = "receiveFromCamelProcess";

  @Deployment
  public static WebArchive createDeployment() {
    return prepareTestDeployment(PROCESS_DEFINITION_KEY, "process/ReceiveFromCamel.bpmn20.xml");
  }

  @Inject
  @Uri("mock:resultEndpoint")
  MockEndpoint resultEndpoint;

  @Produces
  @ApplicationScoped
  public RouteBuilder createRoute() {
    return new RouteBuilder() {
      public void configure() {
        from("direct:sendToCamundaBpm")
          .routeId("receive-from-camel-route")
          .to("log:org.camunda.bpm.camel.cdi?level=INFO&showAll=true&multiline=true")
          .to("camunda-bpm://signal?processDefinitionKey=receiveFromCamelProcess&activityId=waitForCamel")
          .to("log:org.camunda.bpm.camel.cdi?level=INFO&showAll=true&multiline=true")
          .to(resultEndpoint)
        ;
      }
    };
  }

  @Test
  public void doTest() throws InterruptedException {
    Map<String, Object> processVariables = new HashMap<String, Object>();
    processVariables.put("var1", "foo");
    processVariables.put("var2", "bar");
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("receiveFromCamelProcess", processVariables);

    // Verify that a process instance has executed and there is one instance executing now
    assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionKey("receiveFromCamelProcess").count()).isEqualTo(1);
    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("receiveFromCamelProcess").count()).isEqualTo(1);

    /*
     * We need the process instance ID to be able to send the message to it
     *
     * FIXME: we need to fix this with the process execution id or even better with the Activity Instance Model
     * http://camundabpm.blogspot.de/2013/06/introducing-activity-instance-model-to.html
     */
    ProducerTemplate tpl = camelContextBootstrap.getCamelContext().createProducerTemplate();
    tpl.sendBodyAndProperty("direct:sendToCamundaBpm", null, EXCHANGE_HEADER_PROCESS_INSTANCE_ID, processInstance.getId());

    // Assert that the camunda BPM process instance ID has been added as a property to the message
    assertThat(resultEndpoint.assertExchangeReceived(0).getProperty(EXCHANGE_HEADER_PROCESS_INSTANCE_ID)).isEqualTo(processInstance.getId());

    // Assert that the process instance is finished
    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("receiveFromCamelProcess").count()).isEqualTo(0);
  }
}