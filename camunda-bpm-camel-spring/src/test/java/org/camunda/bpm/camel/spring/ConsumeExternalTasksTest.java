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

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.Processor;
import org.apache.camel.component.mock.MockEndpoint;
import org.camunda.bpm.engine.ExternalTaskService;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.externaltask.ExternalTask;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:consume-external-tasks-config.xml")
public class ConsumeExternalTasksTest {

    private MockEndpoint mockEndpoint;

    @Autowired(required = true)
    private CamelContext camelContext;

    @Autowired(required = true)
    private RuntimeService runtimeService;

    @Autowired(required = true)
    private HistoryService historyService;

    @Autowired(required = true)
    private ExternalTaskService externalTaskService;

    @Autowired(required = true)
    @Rule
    public ProcessEngineRule processEngineRule;

    @Before
    public void setUp() throws Exception {

        mockEndpoint = (MockEndpoint) camelContext.getEndpoint("mock:endpoint");
        mockEndpoint.reset();

        // start consumer if stopped by previous test (see "tearDown()")
        // ((BatchConsumer)
        // camelContext.getRoute("firstRoute").getConsumer()).start();

    }

    @After
    public void tearDown() throws Exception {

        // avoid accessing during shutdown of Camunda engine
        // ((BatchConsumer)
        // camelContext.getRoute("firstRoute").getConsumer()).stop();

    }

    @SuppressWarnings("unchecked")
    @Test
    @Deployment(resources = { "process/StartExternalTask.bpmn20.xml" })
    public void testSetProcessVariables() throws Exception {

        // variables to be set by the Camel-endpoint processing the external
        // task
        mockEndpoint.returnReplyBody(new Expression() {
            @Override
            public <T> T evaluate(Exchange exchange, Class<T> type) {
                Map<String, Object> variables = exchange.getIn().getBody(Map.class);
                final String var2 = (String) variables.get("var2");

                final HashMap<String, Object> result = new HashMap<String, Object>();
                result.put("var2", var2 + "bar");
                result.put("var3", "bar3");
                return (T) result;
            }
        });

        // start process
        final Map<String, Object> processVariables = new HashMap<String, Object>();
        processVariables.put("var1", "foo");
        processVariables.put("var2", "bar");
        final ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startExternalTaskProcess",
                processVariables);
        assertThat(processInstance).isNotNull();

        // wait for the external task to be completed
        Thread.sleep(1000);

        // external task is not open any more
        final long externalTasksCount = externalTaskService.createExternalTaskQuery().processInstanceId(
                processInstance.getId()).active().count();
        assertThat(externalTasksCount).isEqualTo(0);

        // assert that the camunda BPM process instance ID has been added as a
        // property to the message
        assertThat(mockEndpoint.assertExchangeReceived(0).getProperty(CAMUNDA_BPM_PROCESS_INSTANCE_ID)).isEqualTo(
                processInstance.getId());

        // all process instance variables are loaded since no "variablesToFetch"
        // parameter was given
        assertThat(mockEndpoint.assertExchangeReceived(0).getIn().getBody()).isNotNull();
        assertThat(mockEndpoint.assertExchangeReceived(0).getIn().getBody()).isInstanceOf(Map.class);
        assertThat(mockEndpoint.assertExchangeReceived(0).getIn().getBody(Map.class).size()).isEqualTo(2);
        assertThat(mockEndpoint.assertExchangeReceived(0).getIn().getBody(Map.class)).containsKey("var1");
        assertThat(mockEndpoint.assertExchangeReceived(0).getIn().getBody(Map.class)).containsKey("var2");
        assertThat(mockEndpoint.assertExchangeReceived(0).getIn().getBody(Map.class)).containsValue("foo");
        assertThat(mockEndpoint.assertExchangeReceived(0).getIn().getBody(Map.class)).containsValue("bar");

        // assert that the variables sent in the response-message has been set
        // into the process
        final List<HistoricVariableInstance> variables = historyService.createHistoricVariableInstanceQuery().processInstanceId(
                processInstance.getId()).list();
        assertThat(variables.size()).isEqualTo(3);
        final HashMap<String, Object> variablesAsMap = new HashMap<String, Object>();
        for (final HistoricVariableInstance variable : variables) {
            variablesAsMap.put(variable.getName(), variable.getValue());
        }
        assertThat(variablesAsMap.containsKey("var1")).isTrue();
        assertThat(variablesAsMap.get("var1")).isEqualTo("foo");
        assertThat(variablesAsMap.containsKey("var2")).isTrue();
        assertThat(variablesAsMap.get("var2")).isEqualTo("barbar");
        assertThat(variablesAsMap.containsKey("var3")).isTrue();
        assertThat(variablesAsMap.get("var3")).isEqualTo("bar3");

        // assert that process in end event "HappyEnd"
        assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(
                processInstance.getId()).activityId("HappyEnd").singleResult()).isNotNull();

        // assert that process ended not due to error boundary event 4711
        assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(
                processInstance.getId()).activityId("End4711").singleResult()).isNull();

        // assert that process ended not due to error boundary event 0815
        assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(
                processInstance.getId()).activityId("End0815").singleResult()).isNull();

    }

    @SuppressWarnings("unchecked")
    @Test
    @Deployment(resources = { "process/StartExternalTask3.bpmn20.xml" })
    public void testLoadNoProcessVariablesAndCompleteTaskIsTrue() throws Exception {

        // variables to be set by the Camel-endpoint processing the external
        // task
        mockEndpoint.returnReplyBody(new Expression() {
            @Override
            public <T> T evaluate(Exchange exchange, Class<T> type) {
                final HashMap<String, Object> result = new HashMap<String, Object>();
                result.put("var1", "foo1");
                result.put("var2", "bar2");
                return (T) result;
            }
        });

        // start process
        final Map<String, Object> processVariables = new HashMap<String, Object>();
        processVariables.put("var1", "foo");
        processVariables.put("var2", "bar");
        final ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startExternalTaskProcess3",
                processVariables);
        assertThat(processInstance).isNotNull();

        // wait for the external task to be completed
        Thread.sleep(1000);

        // external task is not open any more
        final long externalTasksCount = externalTaskService.createExternalTaskQuery().processInstanceId(
                processInstance.getId()).active().count();
        assertThat(externalTasksCount).isEqualTo(0);

        // assert that the camunda BPM process instance ID has been added as a
        // property to the message
        assertThat(mockEndpoint.assertExchangeReceived(0).getProperty(CAMUNDA_BPM_PROCESS_INSTANCE_ID)).isEqualTo(
                processInstance.getId());

        // no process instance variables are loaded since an empty
        // "variablesToFetch" parameter was given
        assertThat(mockEndpoint.assertExchangeReceived(0).getIn().getBody()).isNotNull();
        assertThat(mockEndpoint.assertExchangeReceived(0).getIn().getBody()).isInstanceOf(Map.class);
        assertThat(mockEndpoint.assertExchangeReceived(0).getIn().getBody(Map.class).size()).isEqualTo(0);

        // assert that the variables sent in the response-message has been set
        // into the process
        final List<HistoricVariableInstance> variables = historyService.createHistoricVariableInstanceQuery().processInstanceId(
                processInstance.getId()).list();
        assertThat(variables.size()).isEqualTo(2);
        final HashMap<String, Object> variablesAsMap = new HashMap<String, Object>();
        for (final HistoricVariableInstance variable : variables) {
            variablesAsMap.put(variable.getName(), variable.getValue());
        }
        assertThat(variablesAsMap.containsKey("var1")).isTrue();
        assertThat(variablesAsMap.get("var1")).isEqualTo("foo1");
        assertThat(variablesAsMap.containsKey("var2")).isTrue();
        assertThat(variablesAsMap.get("var2")).isEqualTo("bar2");

        // assert that process in end event "HappyEnd"
        assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(
                processInstance.getId()).activityId("HappyEnd").singleResult()).isNotNull();

        // assert that process ended not due to error boundary event 4711
        assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(
                processInstance.getId()).activityId("End4711").singleResult()).isNull();

        // assert that process ended not due to error boundary event 0815
        assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(
                processInstance.getId()).activityId("End0815").singleResult()).isNull();

    }

    @SuppressWarnings("unchecked")
    @Test
    @Deployment(resources = { "process/StartExternalTask2.bpmn20.xml" })
    public void testCompleteTaskIsFalseAndLockDuration() throws Exception {

        // variables returned but must not be set since task will not be
        // completed
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
        processVariables.put("var3", "foobar");
        final ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startExternalTaskProcess2",
                processVariables);
        assertThat(processInstance).isNotNull();

        // wait for the external task to be completed
        Thread.sleep(1000);

        // external task is still not resolved and locked
        final List<ExternalTask> externalTasks1 = externalTaskService.createExternalTaskQuery().processInstanceId(
                processInstance.getId()).list();
        assertThat(externalTasks1).isNotNull();
        assertThat(externalTasks1.size()).isEqualTo(1);
        assertThat(externalTasks1.get(0).getWorkerId()).isEqualTo("0815");
        assertThat(externalTasks1.get(0).getLockExpirationTime()).isAfter(new Date());

        // wait for the task to unlock and refetch by Camel
        Thread.sleep(2000);

        // assert that the camunda BPM process instance ID has been added as a
        // property to the message for both exchanges received
        assertThat(mockEndpoint.assertExchangeReceived(0).getProperty(CAMUNDA_BPM_PROCESS_INSTANCE_ID)).isEqualTo(
                processInstance.getId());
        assertThat(mockEndpoint.assertExchangeReceived(1).getProperty(CAMUNDA_BPM_PROCESS_INSTANCE_ID)).isEqualTo(
                processInstance.getId());

        // only two process instance variables are loaded according configured
        // value of "variablesToFetch" parameter
        assertThat(mockEndpoint.assertExchangeReceived(0).getIn().getBody()).isNotNull();
        assertThat(mockEndpoint.assertExchangeReceived(0).getIn().getBody()).isInstanceOf(Map.class);
        assertThat(mockEndpoint.assertExchangeReceived(0).getIn().getBody(Map.class).size()).isEqualTo(2);
        assertThat(mockEndpoint.assertExchangeReceived(0).getIn().getBody(Map.class)).containsKey("var2");
        assertThat(mockEndpoint.assertExchangeReceived(0).getIn().getBody(Map.class)).containsKey("var3");
        assertThat(mockEndpoint.assertExchangeReceived(0).getIn().getBody(Map.class)).containsValue("bar");
        assertThat(mockEndpoint.assertExchangeReceived(0).getIn().getBody(Map.class)).containsValue("foobar");

        // assert that the variables sent in the response-message has NOT been
        // set into the process
        final List<HistoricVariableInstance> variables = historyService.createHistoricVariableInstanceQuery().processInstanceId(
                processInstance.getId()).list();
        assertThat(variables.size()).isEqualTo(3);
        final HashMap<String, Object> variablesAsMap = new HashMap<String, Object>();
        for (final HistoricVariableInstance variable : variables) {
            variablesAsMap.put(variable.getName(), variable.getValue());
        }
        assertThat(variablesAsMap.containsKey("var1")).isTrue();
        assertThat(variablesAsMap.get("var1")).isEqualTo("foo");
        assertThat(variablesAsMap.containsKey("var2")).isTrue();
        assertThat(variablesAsMap.get("var2")).isEqualTo("bar");
        assertThat(variablesAsMap.containsKey("var3")).isTrue();
        assertThat(variablesAsMap.get("var3")).isEqualTo("foobar");

        // assert that process NOT in end event "HappyEnd"
        assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(
                processInstance.getId()).activityId("HappyEnd").singleResult()).isNull();

        // assert that process ended not due to error boundary event 4711
        assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(
                processInstance.getId()).activityId("End4711").singleResult()).isNull();

        // assert that process ended not due to error boundary event 0815
        assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(
                processInstance.getId()).activityId("End0815").singleResult()).isNull();

    }

    @SuppressWarnings("unchecked")
    @Test
    @Deployment(resources = { "process/StartExternalTask.bpmn20.xml" })
    public void testBpmnError() throws Exception {

        // variables to be set by the Camel-endpoint processing the external
        // task
        mockEndpoint.returnReplyBody(new Expression() {
            @Override
            public <T> T evaluate(Exchange exchange, Class<T> type) {
                return (T) "4711";
            }
        });

        // start process
        final Map<String, Object> processVariables = new HashMap<String, Object>();
        processVariables.put("var1", "foo");
        processVariables.put("var2", "bar");
        final ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startExternalTaskProcess",
                processVariables);
        assertThat(processInstance).isNotNull();

        // wait for the external task to be completed
        Thread.sleep(1000);

        // external task is still not resolved
        final long externalTasksCount = externalTaskService.createExternalTaskQuery().processInstanceId(
                processInstance.getId()).active().count();
        assertThat(externalTasksCount).isEqualTo(0);

        // assert that the camunda BPM process instance ID has been added as a
        // property to the message
        assertThat(mockEndpoint.assertExchangeReceived(0).getProperty(CAMUNDA_BPM_PROCESS_INSTANCE_ID)).isEqualTo(
                processInstance.getId());

        // assert that the variables sent in the response-message has been set
        // into the process
        final List<HistoricVariableInstance> variables = historyService.createHistoricVariableInstanceQuery().processInstanceId(
                processInstance.getId()).list();
        assertThat(variables.size()).isEqualTo(2);
        final HashMap<String, Object> variablesAsMap = new HashMap<String, Object>();
        for (final HistoricVariableInstance variable : variables) {
            variablesAsMap.put(variable.getName(), variable.getValue());
        }
        assertThat(variablesAsMap.containsKey("var1")).isTrue();
        assertThat(variablesAsMap.get("var1")).isEqualTo("foo");
        assertThat(variablesAsMap.containsKey("var2")).isTrue();
        assertThat(variablesAsMap.get("var2")).isEqualTo("bar");

        // assert that process ended
        final HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(
                processInstance.getId()).singleResult();
        assertThat(historicProcessInstance.getEndTime()).isNotNull();

        // assert that process ended due to error boundary event 4711
        assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(
                processInstance.getId()).activityId("End4711").singleResult()).isNotNull();

        // assert that process not in end event "HappyEnd"
        assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(
                processInstance.getId()).activityId("HappyEnd").singleResult()).isNull();

        // assert that process ended not due to error boundary event 0815
        assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(
                processInstance.getId()).activityId("End0815").singleResult()).isNull();

    }

    @Test
    @Deployment(resources = { "process/StartExternalTask.bpmn20.xml" })
    public void testIncidentAndRetryTimeouts() throws Exception {

        // variables to be set by the Camel-endpoint processing the external
        // task
        mockEndpoint.whenAnyExchangeReceived(new Processor() {
            @Override
            public void process(final Exchange exchange) throws Exception {
                throw new RuntimeException("fail!");
            }
        });

        // count incidents for later comparison
        final long incidentCount = runtimeService.createIncidentQuery().count();

        // start process
        final Map<String, Object> processVariables = new HashMap<String, Object>();
        processVariables.put("var1", "foo");
        processVariables.put("var2", "bar");
        final ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startExternalTaskProcess",
                processVariables);
        assertThat(processInstance).isNotNull();

        // wait for the external task to be completed
        Thread.sleep(1000);

        // external task is still not resolved
        final List<ExternalTask> externalTasks1 = externalTaskService.createExternalTaskQuery().processInstanceId(
                processInstance.getId()).list();
        assertThat(externalTasks1).isNotNull();
        assertThat(externalTasks1.size()).isEqualTo(1);
        assertThat(externalTasks1.get(0).getWorkerId()).isEqualTo(
                "camunda-bpm://externalTask?delay=250&maxTasksPerPoll=5&retries=2&retryTimeout=2s&retryTimeouts=1s&topic=topic1");
        assertThat(externalTasks1.get(0).getRetries()).isEqualTo(2);

        // wait for the next try
        Thread.sleep(1000);

        // external task is still not resolved
        final List<ExternalTask> externalTasks2 = externalTaskService.createExternalTaskQuery().processInstanceId(
                processInstance.getId()).list();
        assertThat(externalTasks2).isNotNull();
        assertThat(externalTasks2.size()).isEqualTo(1);
        assertThat(externalTasks2.get(0).getRetries()).isEqualTo(1);

        // next try is 2 seconds so after 1 second nothing changes
        Thread.sleep(1000);

        // external task is still not resolved
        final List<ExternalTask> externalTasks3 = externalTaskService.createExternalTaskQuery().processInstanceId(
                processInstance.getId()).list();
        assertThat(externalTasks3).isNotNull();
        assertThat(externalTasks3.size()).isEqualTo(1);
        assertThat(externalTasks3.get(0).getRetries()).isEqualTo(1);

        // wait for the next try
        Thread.sleep(1000);

        // external task is still not resolved
        final List<ExternalTask> externalTasks4 = externalTaskService.createExternalTaskQuery().processInstanceId(
                processInstance.getId()).list();
        assertThat(externalTasks4).isNotNull();
        assertThat(externalTasks4.size()).isEqualTo(1);
        assertThat(externalTasks4.get(0).getRetries()).isEqualTo(0);

        // assert that the camunda BPM process instance ID has been added as a
        // property to the message
        assertThat(mockEndpoint.assertExchangeReceived(0).getProperty(CAMUNDA_BPM_PROCESS_INSTANCE_ID)).isEqualTo(
                processInstance.getId());

        // assert that the variables sent in the response-message has been set
        // into the process
        final List<HistoricVariableInstance> variables = historyService.createHistoricVariableInstanceQuery().processInstanceId(
                processInstance.getId()).list();
        assertThat(variables.size()).isEqualTo(2);
        final HashMap<String, Object> variablesAsMap = new HashMap<String, Object>();
        for (final HistoricVariableInstance variable : variables) {
            variablesAsMap.put(variable.getName(), variable.getValue());
        }
        assertThat(variablesAsMap.containsKey("var1")).isTrue();
        assertThat(variablesAsMap.get("var1")).isEqualTo("foo");
        assertThat(variablesAsMap.containsKey("var2")).isTrue();
        assertThat(variablesAsMap.get("var2")).isEqualTo("bar");

        // assert that process not ended
        final HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(
                processInstance.getId()).singleResult();
        assertThat(historicProcessInstance.getEndTime()).isNull();

        // assert that incident raised
        assertThat(runtimeService.createIncidentQuery().count()).isEqualTo(incidentCount + 1);

    }

}
