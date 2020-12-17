package org.camunda.bpm.camel.cdi;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.cdi.Uri;
import org.apache.camel.component.mock.MockEndpoint;
import org.camunda.bpm.engine.task.Task;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import static org.fest.assertions.api.Assertions.assertThat;

/*
 * This test is basically a ... smoke test. It's a baseline to check that all the moving pieces 
 * (Maven deps, Arquillian config, ...) are working OK:
 *   - process definition deployment
 *   - Camel context instantiation and route startup
 *   - ...
 *   
 *   See other tests in this package for actual exercising of the camunda BPM <-> Camel integration
 */
@RunWith(Arquillian.class)
public class SmokeIT extends BaseArquillianIntegrationTest {

  private static String PROCESS_DEFINITION_KEY = "smokeTestProcess";

  @Deployment
  public static WebArchive createDeployment() {
    return prepareTestDeployment(PROCESS_DEFINITION_KEY, "process/SmokeTest.bpmn20.xml");
  }

  @Inject
  @Uri("resultEndpoint")
  MockEndpoint resultEndpoint;

  @Produces
  @ApplicationScoped
  public RouteBuilder createRoute() {
    return new RouteBuilder() {
      public void configure() {
        from("timer://smoke-message?repeatCount=1")
          .routeId("smoke-test-route")
          .to("log:org.camunda.bpm.camel.cdi?level=INFO&showAll=true&multiline=true")
          .to(resultEndpoint)
        ;
      }
    };
  }

  @Test
  public void doTest() throws InterruptedException {

    assertThat(camelContextBootstrap).isNotNull();
    assertThat(camelContextBootstrap.getCamelContext()).isNotNull();
    assertThat(camelService).isNotNull();

    runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY);
    Task task = taskService.createTaskQuery().processDefinitionKey(PROCESS_DEFINITION_KEY).singleResult();
    assertThat(task).isNotNull();
    assertThat("My Task").isEqualTo(task.getName());
    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey(PROCESS_DEFINITION_KEY).count()).isEqualTo(1);

    taskService.complete(task.getId());
    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey(PROCESS_DEFINITION_KEY).count()).isEqualTo(0);
    resultEndpoint.expectedMessageCount(1);
    resultEndpoint.assertIsSatisfied();
  }
}
