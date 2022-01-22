package org.camunda.bpm.camel.cdi;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.cdi.Uri;
import org.apache.camel.component.mock.MockEndpoint;
import org.camunda.bpm.engine.runtime.ProcessInstance;
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
import static org.camunda.bpm.camel.component.CamundaBpmConstants.*;

@RunWith(Arquillian.class)
public class SendToCamelIT extends BaseArquillianIntegrationTest {

  private static String PROCESS_DEFINITION_KEY = "sendToCamelProcess";

  @Deployment
  public static WebArchive createDeployment() {
    return prepareTestDeployment(PROCESS_DEFINITION_KEY, "process/SendToCamel.bpmn20.xml");
  }

  @Inject
  @Uri("mock:resultEndpoint")
  MockEndpoint resultEndpoint;

  @Produces
  @ApplicationScoped
  public RouteBuilder createRoute() {
    return new RouteBuilder() {
      public void configure() {
        from("direct:sendToCamelServiceTask")
          .routeId("send-to-camel-route")
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
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("sendToCamelProcess", processVariables);

    // Verify that a process instance was executed and there are no instances executing now
    assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionKey("sendToCamelProcess").count()).isEqualTo(1);
    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("sendToCamelProcess").count()).isEqualTo(0);

    // Assert that the camunda BPM process instance ID has been added as a property to the message
    assertThat(resultEndpoint.assertExchangeReceived(0).getProperty(EXCHANGE_HEADER_PROCESS_INSTANCE_ID)).isEqualTo(processInstance.getId());

    // Assert that the body of the message received by the endpoint contains a hash map with the value of the process variable 'var1' sent from camunda BPM
    assertThat(resultEndpoint.assertExchangeReceived(0).getIn().getBody(String.class)).isEqualTo("{var1=foo}");

    // FIXME: check that var2 is also present as a property!
  }
}