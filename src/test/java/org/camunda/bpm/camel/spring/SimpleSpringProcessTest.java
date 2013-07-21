/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.camunda.bpm.camel.spring;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:spring-camel-activiti-context.xml")
public class SimpleSpringProcessTest {

  MockEndpoint service1;

  MockEndpoint service2;

  @Autowired(required = true)
  ApplicationContext applicationContext;

  @Autowired(required = true)
  CamelContext camelContext;

  @Autowired(required = true)
  RuntimeService runtimeService;

  @Autowired(required = true)
  @Rule
  public ProcessEngineRule processEngineRule;

  @Before
  public void setUp() {
    service1 = (MockEndpoint) camelContext.getEndpoint("mock:service1");
    service1.reset();
    service2 = (MockEndpoint) camelContext.getEndpoint("mock:service2");
    service2.reset();
  }

  @Test
  @Deployment(resources = {"process/example.bpmn20.xml"})
  public void testRunProcess() throws Exception {
    ProducerTemplate tpl = camelContext.createProducerTemplate();
    service1.expectedBodiesReceived("ala");

    Map<String, Object> processVariables = new HashMap<String, Object>();
    processVariables.put("var1", "ala");
    runtimeService.startProcessInstanceByKey("camelProcess", processVariables);
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);

    //String instanceId = (String) tpl.requestBody("direct:start", Collections.singletonMap("var1", "ala"));

    //tpl.sendBodyAndProperty("direct:receive", null, ActivitiProducer.PROCESS_ID_PROPERTY, instanceId);

    //assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(0);

    //service1.assertIsSatisfied();
    //Map m = service2.getExchanges().get(0).getIn().getBody(Map.class);
    //assertThat(m.get("var1")).isEqualTo("ala");
    //assertThat(m.get("var2")).isEqualTo("var2");
  }

  //@Test
//  @Deployment(resources = {"process/example.bpmn20.xml"})
//  public void testRunProcessByKey() throws Exception {
//    //CamelContext camelContext = applicationContext.getBean(CamelContext.class);
//    ProducerTemplate tpl = camelContext.createProducerTemplate();
//    MockEndpoint me = (MockEndpoint) camelContext.getEndpoint("mock:service1");
//    me.expectedBodiesReceived("ala");
//
//
//    tpl.sendBodyAndProperty("direct:start", Collections.singletonMap("var1", "ala"), ActivitiProducer.PROCESS_KEY_PROPERTY, "key1");
//
//    String instanceId = runtimeService.createProcessInstanceQuery().processInstanceBusinessKey("key1")
//        .singleResult().getProcessInstanceId();
//    tpl.sendBodyAndProperty("direct:receive", null, ActivitiProducer.PROCESS_KEY_PROPERTY, "key1");
//
//    //assertProcessEnded(instanceId);
//    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(0);
//    me.assertIsSatisfied();
//  }

}
