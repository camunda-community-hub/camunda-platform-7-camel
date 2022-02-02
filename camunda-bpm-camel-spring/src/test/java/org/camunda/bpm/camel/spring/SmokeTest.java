package org.camunda.bpm.camel.spring;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.Assertions;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:smoke-test-config.xml")
public class SmokeTest {

  @Autowired(required = true)
  RuntimeService runtimeService;

  @Autowired(required = true)
  TaskService taskService;

  @Autowired
  @Rule
  public ProcessEngineRule processEngineRule;

  @Test
  @Deployment(resources = {"process/SmokeTest.bpmn20.xml"} )
  public void smokeTest() throws Exception {
    runtimeService.startProcessInstanceByKey("smokeTestProcess");
    Task task = taskService.createTaskQuery().singleResult();
    assertThat("My Task").isEqualTo(task.getName());

    taskService.complete(task.getId());
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(0);
  }
}
