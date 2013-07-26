package org.camunda.bpm.camel.cdi;

import org.camunda.bpm.engine.task.Task;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(Arquillian.class)
public class SmokeIT extends BaseArquillianIntegrationTest {

  private static String PROCESS_DEFINITION_KEY = "smokeTestProcess";

  @Deployment
  public static WebArchive createDeployment() {
    return prepareTestDeployment(PROCESS_DEFINITION_KEY, "process/SmokeTest.bpmn20.xml", SmokeRoute.class);
  }

  @Test
  public void testDeploymentAndStartInstance() throws InterruptedException {

    assertThat(camelContextBootstrap).isNotNull();
    assertThat(camelContextBootstrap.getCamelContext()).isNotNull();
    assertThat(camelService).isNotNull();

    runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY);
    Task task = taskService.createTaskQuery().processDefinitionKey(PROCESS_DEFINITION_KEY).singleResult();
    assertThat(task).isNotNull();
    assertThat("My Task").isEqualTo(task.getName());

    taskService.complete(task.getId());
    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey(PROCESS_DEFINITION_KEY).count()).isEqualTo(0);
  }
}
