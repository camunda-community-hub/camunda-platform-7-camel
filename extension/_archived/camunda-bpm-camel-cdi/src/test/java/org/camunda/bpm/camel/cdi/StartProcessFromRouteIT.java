package org.camunda.bpm.camel.cdi;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.cdi.Uri;
import org.apache.camel.component.mock.MockEndpoint;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import java.util.Collections;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.*;

@RunWith(Arquillian.class)
public class StartProcessFromRouteIT extends BaseArquillianIntegrationTest {

  private static String PROCESS_DEFINITION_KEY = "startProcessFromRoute";

  @Deployment
  public static WebArchive createDeployment() {
    return prepareTestDeployment(PROCESS_DEFINITION_KEY, "process/StartProcessFromRoute.bpmn20.xml");
  }

  @Inject
  @Uri("mock:mockEndpoint")
  MockEndpoint mockEndpoint;
  
  @Inject
  @Uri("mock:processVariableEndpoint")
  MockEndpoint processVariableEndpoint;

  @Produces
  @ApplicationScoped
  public RouteBuilder createRoute() {
    return new RouteBuilder() {
      public void configure() {
        from("direct:start")
          .routeId("start-process-from-route")
          .to("log:org.camunda.bpm.camel.cdi?level=INFO&showAll=true&multiline=true")
          .to("camunda-bpm://start?processDefinitionKey=startProcessFromRoute&copyBodyAsVariable=var1")
          .to("log:org.camunda.bpm.camel.cdi?level=INFO&showAll=true&multiline=true")
          .to(mockEndpoint)
        ;

        from("direct:processVariable")
          .routeId("processVariableRoute")
          .to("log:org.camunda.bpm.camel.cdi?level=INFO&showAll=true&multiline=true")
          .to(processVariableEndpoint)
        ;
      }
    };
  }

  @Test
  public void doTest() throws InterruptedException {
    ProducerTemplate tpl = camelContextBootstrap.getCamelContext().createProducerTemplate();

    String processInstanceId = (String) tpl.requestBody("direct:start", Collections.singletonMap("var1", "valueOfVar1"));
    assertThat(processInstanceId).isNotNull();
    System.out.println("Process instance ID: " + processInstanceId);

    // Verify that a process instance was executed and there are no instances executing now
    assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionKey("startProcessFromRoute").count()).isEqualTo(1);
    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("startProcessFromRoute").count()).isEqualTo(0);

    // Assert that the camunda BPM process instance ID has been added as a property to the message
    assertThat(mockEndpoint.assertExchangeReceived(0).getProperty(EXCHANGE_HEADER_PROCESS_INSTANCE_ID)).isEqualTo(processInstanceId);

    // The body of the message comming out from the camunda-bpm:<process definition> endpoint is the process instance
    assertThat(mockEndpoint.assertExchangeReceived(0).getIn().getBody(String.class)).isEqualTo(processInstanceId);

    // We should receive a hash map with the value of 'var1' as the body of the message
    assertThat(processVariableEndpoint.assertExchangeReceived(0).getIn().getBody(String.class)).isEqualTo("{var1=valueOfVar1}");
  }
}