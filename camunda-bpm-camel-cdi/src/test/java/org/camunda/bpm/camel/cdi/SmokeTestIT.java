package org.camunda.bpm.camel.cdi;

import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.task.Task;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.jboss.shrinkwrap.resolver.api.DependencyResolvers;
import org.jboss.shrinkwrap.resolver.api.maven.MavenDependencyResolver;

import javax.inject.Inject;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(Arquillian.class)
public class SmokeTestIT {

  @Deployment
  public static WebArchive createDeployment() {
    MavenDependencyResolver resolver = DependencyResolvers.use(MavenDependencyResolver.class)
        .loadMetadataFromPom("pom.xml");

    return ShrinkWrap.create(WebArchive.class, "process-smoke-test.war")
        .addAsLibraries(resolver.artifact("org.camunda.bpm:camunda-engine-cdi").resolveAsFiles())
        .addAsLibraries(resolver.artifact("org.camunda.bpm.javaee:camunda-ejb-client").resolveAsFiles())
        .addAsLibraries(resolver.artifact("org.easytesting:fest-assert-core").resolveAsFiles())

        .addAsWebResource("META-INF/processes.xml", "WEB-INF/classes/META-INF/processes.xml")
        .addAsResource("process/SmokeTest.bpmn20.xml")
        ;
  }

  @Inject
  @SuppressWarnings("cdi-ambiguous-dependency")
  private RuntimeService runtimeService;

  @Inject
  @SuppressWarnings("cdi-ambiguous-dependency")
  private TaskService taskService;

  private static String PROCESS_DEFINITION_KEY = "smokeTestProcess";

  @Test
  public void testDeploymentAndStartInstance() throws InterruptedException {
    runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY);
    Task task = taskService.createTaskQuery().processDefinitionKey(PROCESS_DEFINITION_KEY).singleResult();
    assertThat(task).isNotNull();
    assertThat("My Task").isEqualTo(task.getName());

    taskService.complete(task.getId());
    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey(PROCESS_DEFINITION_KEY).count()).isEqualTo(0);
  }
}
