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

import static org.camunda.bpm.camel.component.CamundaBpmConstants.CAMUNDA_BPM_PROCESS_INSTANCE_ID;
import static org.fest.assertions.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.component.mock.MockEndpoint;
import org.camunda.bpm.engine.ExternalTaskService;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.externaltask.ExternalTask;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
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

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:consume-external-tasks-config.xml")
public class ConsumeExternalTasksTest {

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
    ExternalTaskService externalTaskService;

    @Autowired(required = true)
    @Rule
    public ProcessEngineRule processEngineRule;

    @Before
    public void setUp() {
    	
        mockEndpoint = (MockEndpoint) camelContext.getEndpoint("mock:endpoint");
        mockEndpoint.reset();
        
    }

    @SuppressWarnings("unchecked")
	@Test
    @Deployment(resources = {"process/StartExternalTask.bpmn20.xml"})
    public void testBaseFunctionality() throws Exception {

    	// variables to be set by the Camel-endpoint processing the external task
        mockEndpoint.returnReplyBody(new Expression() {
			@Override
			public <T> T evaluate(Exchange exchange, Class<T> type) {
				final HashMap<String, Object> result = new HashMap<String, Object>();
				result.put("var2", "bar2");
				result.put("var3", "bar3");
				return (T) result;
			}
		});
    	
    	// start process
    	final Map<String, Object> processVariables = new HashMap<String, Object>();
    	processVariables.put("var1", "foo");
    	processVariables.put("var2", "bar");
    	final ProcessInstance processInstance = 
    			runtimeService.startProcessInstanceByKey("startExternalTaskProcess", processVariables);
    	assertThat(processInstance).isNotNull();
    	
    	// wait for the external task to be completed
    	Thread.sleep(1000);

    	final List<ExternalTask> externalTasks = externalTaskService.createExternalTaskQuery().list();
    	assertThat(externalTasks).isNotNull();
    	assertThat(externalTasks.size()).isEqualTo(0);
    	
    	// assert that the camunda BPM process instance ID has been added as a property to the message
    	assertThat(mockEndpoint.assertExchangeReceived(0).getProperty(
    			CAMUNDA_BPM_PROCESS_INSTANCE_ID)).isEqualTo(processInstance.getId());
    	
    	// assert that the variables sent in the response-message has been set into the process
        final List<HistoricVariableInstance> variables = historyService
        		.createHistoricVariableInstanceQuery()
        		.processInstanceId(processInstance.getId())
        		.list();
        assertThat(variables.size()).isEqualTo(3);
        final HashMap<String, Object> variablesAsMap = new HashMap<String, Object>();
        for (final HistoricVariableInstance variable : variables) {
        	variablesAsMap.put(variable.getName(), variable.getValue());
        }
        assertThat(variablesAsMap.containsKey("var1")).isTrue();
        assertThat(variablesAsMap.get("var1")).isEqualTo("foo");
        assertThat(variablesAsMap.containsKey("var2")).isTrue();
        assertThat(variablesAsMap.get("var2")).isEqualTo("bar2");
        assertThat(variablesAsMap.containsKey("var3")).isTrue();
        assertThat(variablesAsMap.get("var3")).isEqualTo("bar3");
        
    }

}
