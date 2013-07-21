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

package org.activiti.camel;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Collections;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:start-process-from-route-config.xml")
public class StartProcessFromRouteTest {

  MockEndpoint mockEndpoint;

  @Autowired(required = true)
  CamelContext camelContext;

  @Autowired(required = true)
  RuntimeService runtimeService;

  @Autowired(required = true)
  HistoryService historyService;

  @Autowired(required = true)
  @Rule
  public ProcessEngineRule processEngineRule;

  @Before
  public void setUp() {
    mockEndpoint = (MockEndpoint) camelContext.getEndpoint("mock:endpoint");
    mockEndpoint.reset();
  }

  @Test
  @Deployment(resources = {"process/StartProcessFromRoute.bpmn20.xml"})
  public void doTest() throws Exception {
    ProducerTemplate tpl = camelContext.createProducerTemplate();

    String processInstanceId = (String) tpl.requestBody("direct:start", Collections.singletonMap("var1", "valueOfVar1"));
    assertThat(processInstanceId).isNotNull();
    System.out.println("Process instance ID: " + processInstanceId);

    // Verify that a process instance was executed and there are no instances executing now
    assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionKey("startProcessFromRoute").count()).isEqualTo(1);
    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(0);

    // TODO: Assert that the camunda BPM process instance ID has been added as a property to the message
    assertThat(mockEndpoint.assertExchangeReceived(0).getProperty(ActivitiProducer.PROCESS_ID_PROPERTY)).isEqualTo(processInstanceId);
  }
}
