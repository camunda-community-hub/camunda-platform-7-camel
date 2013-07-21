package org.camunda.bpm.camel.spring;/* Licensed under the Apache License, Version 2.0 (the "License");
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

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
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

import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:send-to-camel-config.xml")
public class SendToCamelTest {

  MockEndpoint mockEndpoint;

  @Autowired(required = true)
  ApplicationContext applicationContext;

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
  @Deployment(resources = {"process/SendToCamel.bpmn20.xml"})
  public void doTest() throws Exception {
    ProducerTemplate tpl = camelContext.createProducerTemplate();
    mockEndpoint.expectedBodiesReceived("ala");

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
  }
}
