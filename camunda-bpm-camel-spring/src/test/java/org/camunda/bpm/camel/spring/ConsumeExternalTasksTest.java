package org.camunda.bpm.camel.spring;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.EXCHANGE_HEADER_ATTEMPTSSTARTED;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.EXCHANGE_HEADER_PROCESS_INSTANCE_ID;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.EXCHANGE_HEADER_RETRIESLEFT;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.assertj.core.api.Assertions;
import org.camunda.bpm.camel.component.CamundaBpmConstants;
import org.camunda.bpm.camel.component.externaltasks.SetExternalTaskRetries;
import org.camunda.bpm.engine.ExternalTaskService;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.externaltask.ExternalTask;
import org.camunda.bpm.engine.externaltask.LockedExternalTask;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.junit.After;
import org.junit.Assert;
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

    @SetExternalTaskRetries(retries = 0)
    public static class CreateIncidentException extends Exception {
        private static final long serialVersionUID = 1L;

        public CreateIncidentException(final String message) {
            super(message);
        }
    };

    @SetExternalTaskRetries(retries = 0, relative = true)
    public static class DontChangeRetriesException extends Exception {
        private static final long serialVersionUID = 1L;

        public DontChangeRetriesException(final String message) {
            super(message);
        }
    };

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
        assertThat(mockEndpoint.assertExchangeReceived(0).getProperty(EXCHANGE_HEADER_PROCESS_INSTANCE_ID)).isEqualTo(
                processInstance.getId());

        // all process instance variables are loaded since no "variablesToFetch"
        // parameter was given
        var exchangeIn = mockEndpoint.assertExchangeReceived(0).getIn();
        assertThat(exchangeIn.getBody()).isNotNull();
        assertThat(exchangeIn.getBody()).isInstanceOf(Map.class);
        assertThat(exchangeIn.getBody(Map.class)).hasSize(2);
        assertThat(exchangeIn.getBody(Map.class)).containsKey("var1");
        assertThat(exchangeIn.getBody(Map.class)).containsKey("var2");
        assertThat(exchangeIn.getBody(Map.class)).containsValue("foo");
        assertThat(exchangeIn.getBody(Map.class)).containsValue("bar");

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
    public void testLoadNoProcessVariablesAndAsyncIsFalse() throws Exception {

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
        assertThat(mockEndpoint.assertExchangeReceived(0).getProperty(EXCHANGE_HEADER_PROCESS_INSTANCE_ID)).isEqualTo(
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
    @Deployment(resources = { "process/StartExternalTask4.bpmn20.xml" })
    public void testCompleteTaskOnCompletionSucessfully() throws Exception {

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

        // external task is still not resolved and not locked
        final List<ExternalTask> externalTasks1 = externalTaskService.createExternalTaskQuery().processInstanceId(
                processInstance.getId()).list();
        assertThat(externalTasks1).isNotNull();
        assertThat(externalTasks1.size()).isEqualTo(1);
        assertThat(externalTasks1.get(0).getWorkerId()).isNull();

        // find external task and lock
        final List<LockedExternalTask> locked = externalTaskService.fetchAndLock(1, "0815", true).topic("topic4",
                5000).execute();
        assertThat(locked).isNotNull();
        assertThat(locked.size()).isEqualTo(1);
        final LockedExternalTask lockedExternalTask = locked.get(0);

        // call route "direct:testRoute"
        final ProducerTemplate template = camelContext.createProducerTemplate();
        template.requestBodyAndHeader("direct:firstTestRoute",
                null,
                "CamundaBpmExternalTaskId",
                lockedExternalTask.getId());

        // ensure endpoint has been called
        assertThat(mockEndpoint.assertExchangeReceived(0)).isNotNull();
        assertThat(mockEndpoint.assertExchangeReceived(0).getIn().getHeader(EXCHANGE_HEADER_ATTEMPTSSTARTED)).isEqualTo(
                0);
        assertThat(mockEndpoint.assertExchangeReceived(0).getIn().getHeader(EXCHANGE_HEADER_RETRIESLEFT)).isEqualTo(2);

        // assert that process in end event "HappyEnd"
        assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(
                processInstance.getId()).activityId("HappyEnd").singleResult()).isNotNull();

        // assert that process ended not due to error boundary event 4711
        assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(
                processInstance.getId()).activityId("End4711").singleResult()).isNull();

        // assert that process ended not due to error boundary event 0815
        assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(
                processInstance.getId()).activityId("End0815").singleResult()).isNull();

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
        assertThat(variablesAsMap.get("var2")).isEqualTo("bar2");
        assertThat(variablesAsMap.containsKey("var3")).isTrue();
        assertThat(variablesAsMap.get("var3")).isEqualTo("bar3");

    }

    @Test
    @Deployment(resources = { "process/StartExternalTask4.bpmn20.xml" })
    public void testCompleteTaskOnCompletionFailure() throws Exception {

        final String FAILURE = "Failure";

        // variables returned but must not be set since task will not be
        // completed
        mockEndpoint.whenAnyExchangeReceived(new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                throw new Exception(FAILURE);
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

        // external task is still not resolved and not locked
        final List<ExternalTask> externalTasks1 = externalTaskService.createExternalTaskQuery().processInstanceId(
                processInstance.getId()).list();
        assertThat(externalTasks1).isNotNull();
        assertThat(externalTasks1.size()).isEqualTo(1);
        assertThat(externalTasks1.get(0).getWorkerId()).isNull();
        assertThat(externalTasks1.get(0).getRetries()).isNull();

        // find external task and lock
        final List<LockedExternalTask> locked = externalTaskService.fetchAndLock(1, "0815", true).topic("topic4",
                5000).execute();
        assertThat(locked).isNotNull();
        assertThat(locked.size()).isEqualTo(1);
        final LockedExternalTask lockedExternalTask = locked.get(0);

        // call route "direct:testRoute"
        final ProducerTemplate template = camelContext.createProducerTemplate();
        try {
            template.requestBodyAndHeader("direct:firstTestRoute",
                    null,
                    "CamundaBpmExternalTaskId",
                    lockedExternalTask.getId());
            Assert.fail("Expected an exception, but Camel route succeeded!");
        } catch (Exception e) {
            // expected
        }

        // ensure endpoint has been called
        assertThat(mockEndpoint.assertExchangeReceived(0)).isNotNull();

        // external task is still not resolved
        final List<ExternalTask> externalTasks2 = externalTaskService.createExternalTaskQuery().processInstanceId(
                processInstance.getId()).list();
        assertThat(externalTasks2).isNotNull();
        assertThat(externalTasks2.size()).isEqualTo(1);
        assertThat(externalTasks2.get(0).getRetries()).isEqualTo(2);

        // assert that process not in the end event "HappyEnd"
        assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(
                processInstance.getId()).activityId("HappyEnd").singleResult()).isNull();

        // assert that process ended not due to error boundary event 4711
        assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(
                processInstance.getId()).activityId("End4711").singleResult()).isNull();

        // assert that process ended not due to error boundary event 0815
        assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(
                processInstance.getId()).activityId("End0815").singleResult()).isNull();

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
        assertThat(variablesAsMap.get("var2")).isEqualTo("bar");
        assertThat(variablesAsMap.containsKey("var3")).isTrue();
        assertThat(variablesAsMap.get("var3")).isEqualTo("foobar");

        // complete task to make test order not relevant
        externalTaskService.complete(externalTasks2.get(0).getId(), "0815");

    }

    @SuppressWarnings("unchecked")
    @Test
    @Deployment(resources = { "process/StartExternalTask4.bpmn20.xml" })
    public void testCompleteTaskSucessfully() throws Exception {

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

        // external task is still not resolved and not locked
        final List<ExternalTask> externalTasks1 = externalTaskService.createExternalTaskQuery().processInstanceId(
                processInstance.getId()).list();
        assertThat(externalTasks1).isNotNull();
        assertThat(externalTasks1.size()).isEqualTo(1);
        assertThat(externalTasks1.get(0).getWorkerId()).isNull();

        // find external task and lock
        final List<LockedExternalTask> locked = externalTaskService.fetchAndLock(1, "0815", true).topic("topic4",
                5000).execute();
        assertThat(locked).isNotNull();
        assertThat(locked.size()).isEqualTo(1);
        final LockedExternalTask lockedExternalTask = locked.get(0);

        // call route "direct:testRoute"
        final ProducerTemplate template = camelContext.createProducerTemplate();
        template.requestBodyAndHeader("direct:secondTestRoute",
                null,
                "CamundaBpmExternalTaskId",
                lockedExternalTask.getId());

        // ensure endpoint has been called
        assertThat(mockEndpoint.assertExchangeReceived(0)).isNotNull();

        // assert that process in end event "HappyEnd"
        assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(
                processInstance.getId()).activityId("HappyEnd").singleResult()).isNotNull();

        // assert that process ended not due to error boundary event 4711
        assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(
                processInstance.getId()).activityId("End4711").singleResult()).isNull();

        // assert that process ended not due to error boundary event 0815
        assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(
                processInstance.getId()).activityId("End0815").singleResult()).isNull();

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
        assertThat(variablesAsMap.get("var2")).isEqualTo("bar2");
        assertThat(variablesAsMap.containsKey("var3")).isTrue();
        assertThat(variablesAsMap.get("var3")).isEqualTo("bar3");

    }

    @Test
    @Deployment(resources = { "process/StartExternalTask4.bpmn20.xml" })
    public void testCompleteTaskFailure() throws Exception {

        // start process
        final Map<String, Object> processVariables = new HashMap<String, Object>();
        processVariables.put("var1", "foo");
        processVariables.put("var2", "bar");
        processVariables.put("var3", "foobar");
        final ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startExternalTaskProcess2",
                processVariables);
        assertThat(processInstance).isNotNull();

        // external task is still not resolved and not locked
        final List<ExternalTask> externalTasks1 = externalTaskService.createExternalTaskQuery().processInstanceId(
                processInstance.getId()).list();
        assertThat(externalTasks1).isNotNull();
        assertThat(externalTasks1.size()).isEqualTo(1);
        assertThat(externalTasks1.get(0).getWorkerId()).isNull();
        assertThat(externalTasks1.get(0).getRetries()).isNull();

        // find external task and lock
        final List<LockedExternalTask> locked = externalTaskService.fetchAndLock(1, "0815", true).topic("topic4",
                5000).execute();
        assertThat(locked).isNotNull();
        assertThat(locked.size()).isEqualTo(1);
        final LockedExternalTask lockedExternalTask = locked.get(0);

        /*
         * test CamundaBpmConstants.EXCHANGE_RESPONSE_IGNORE
         */

        // variables returned but must not be set since task will not be
        // completed
        mockEndpoint.returnReplyBody(new Expression() {
            @SuppressWarnings("unchecked")
            @Override
            public <T> T evaluate(Exchange exchange, Class<T> type) {
                return (T) CamundaBpmConstants.EXCHANGE_RESPONSE_IGNORE;
            }
        });

        // second route does not recognize exceptions

        // call route "direct:secondTestRoute"
        final ProducerTemplate template = camelContext.createProducerTemplate();
        template.requestBodyAndHeader("direct:secondTestRoute",
                null,
                "CamundaBpmExternalTaskId",
                lockedExternalTask.getId());

        // ensure endpoint has been called
        assertThat(mockEndpoint.assertExchangeReceived(0)).isNotNull();

        // external task is still not resolved
        final List<ExternalTask> externalTasks2 = externalTaskService.createExternalTaskQuery().processInstanceId(
                processInstance.getId()).list();
        assertThat(externalTasks2).isNotNull();
        assertThat(externalTasks2.size()).isEqualTo(1);
        // Exception aborted processing so retries could not be set!
        assertThat(externalTasks2.get(0).getRetries()).isNull();
        assertThat(externalTasks2.get(0).getErrorMessage()).isNull();

        // assert that process not in the end event "HappyEnd"
        assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(
                processInstance.getId()).activityId("HappyEnd").singleResult()).isNull();

        // assert that process ended not due to error boundary event 4711
        assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(
                processInstance.getId()).activityId("End4711").singleResult()).isNull();

        // assert that process ended not due to error boundary event 0815
        assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(
                processInstance.getId()).activityId("End0815").singleResult()).isNull();

        // assert that the variables unchanged
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

        /*
         * test common exception
         */

        mockEndpoint.reset();

        final String FAILURE = "FAILURE";

        // variables returned but must not be set since task will not be
        // completed
        mockEndpoint.whenAnyExchangeReceived(new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                throw new Exception(FAILURE);
            }
        });

        // call route "direct:testRoute"
        final ProducerTemplate template2 = camelContext.createProducerTemplate();
        try {
            template2.requestBodyAndHeader("direct:firstTestRoute",
                    null,
                    "CamundaBpmExternalTaskId",
                    lockedExternalTask.getId());
            Assert.fail("Expected an exception, but Camel route succeeded!");
        } catch (Exception e) {
            // expected
        }

        // ensure endpoint has been called
        assertThat(mockEndpoint.assertExchangeReceived(0)).isNotNull();

        // external task is still not resolved
        final List<ExternalTask> externalTasks4 = externalTaskService.createExternalTaskQuery().processInstanceId(
                processInstance.getId()).list();
        assertThat(externalTasks4).isNotNull();
        assertThat(externalTasks4.size()).isEqualTo(1);
        // Exception aborted processing so retries could not be set!
        assertThat(externalTasks4.get(0).getRetries()).isEqualTo(2);
        assertThat(externalTasks4.get(0).getErrorMessage()).isEqualTo(FAILURE);

        // assert that process not in the end event "HappyEnd"
        assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(
                processInstance.getId()).activityId("HappyEnd").singleResult()).isNull();

        // assert that process ended not due to error boundary event 4711
        assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(
                processInstance.getId()).activityId("End4711").singleResult()).isNull();

        // assert that process ended not due to error boundary event 0815
        assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(
                processInstance.getId()).activityId("End0815").singleResult()).isNull();

        // assert that the variables unchanged
        final List<HistoricVariableInstance> variables2 = historyService.createHistoricVariableInstanceQuery().processInstanceId(
                processInstance.getId()).list();
        assertThat(variables2.size()).isEqualTo(3);
        final HashMap<String, Object> variablesAsMap2 = new HashMap<String, Object>();
        for (final HistoricVariableInstance variable : variables2) {
            variablesAsMap2.put(variable.getName(), variable.getValue());
        }
        assertThat(variablesAsMap2.containsKey("var1")).isTrue();
        assertThat(variablesAsMap2.get("var1")).isEqualTo("foo");
        assertThat(variablesAsMap2.containsKey("var2")).isTrue();
        assertThat(variablesAsMap2.get("var2")).isEqualTo("bar");
        assertThat(variablesAsMap2.containsKey("var3")).isTrue();
        assertThat(variablesAsMap2.get("var3")).isEqualTo("foobar");

        // complete task to make test order not relevant
        externalTaskService.complete(externalTasks2.get(0).getId(), "0815");

    }

    @Test
    @Deployment(resources = { "process/StartExternalTask4.bpmn20.xml" })
    public void testSetExternalTaskRetriesAnnotation() throws Exception {

        // start process
        final Map<String, Object> processVariables = new HashMap<String, Object>();
        processVariables.put("var1", "foo");
        processVariables.put("var2", "bar");
        processVariables.put("var3", "foobar");
        final ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startExternalTaskProcess2",
                processVariables);
        assertThat(processInstance).isNotNull();

        // external task is still not resolved and not locked
        final List<ExternalTask> externalTasks1 = externalTaskService.createExternalTaskQuery().processInstanceId(
                processInstance.getId()).list();
        assertThat(externalTasks1).isNotNull();
        assertThat(externalTasks1.size()).isEqualTo(1);
        assertThat(externalTasks1.get(0).getWorkerId()).isNull();
        assertThat(externalTasks1.get(0).getRetries()).isNull();

        // find external task and lock
        final List<LockedExternalTask> locked = externalTaskService.fetchAndLock(1, "0815", true).topic("topic4",
                5000).execute();
        assertThat(locked).isNotNull();
        assertThat(locked.size()).isEqualTo(1);
        final LockedExternalTask lockedExternalTask = locked.get(0);

        // set retries artificially
        externalTaskService.handleFailure(lockedExternalTask.getId(), "0815", "Blabal", 2, 0);

        /*
         * DontChangeRetriesException
         */

        mockEndpoint.reset();

        final String DONTCHANGEMSG = "DoNotChange";

        // variables returned but must not be set since task will not be
        // completed
        mockEndpoint.whenAnyExchangeReceived(new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                throw new DontChangeRetriesException(DONTCHANGEMSG);
            }
        });

        // call route "direct:testRoute"
        final ProducerTemplate template2 = camelContext.createProducerTemplate();
        try {
            template2.requestBodyAndHeader("direct:firstTestRoute",
                    null,
                    "CamundaBpmExternalTaskId",
                    lockedExternalTask.getId());
            Assert.fail("Expected an exception, but Camel route succeeded!");
        } catch (Exception e) {
            // expected
        }

        // ensure endpoint has been called
        assertThat(mockEndpoint.assertExchangeReceived(0)).isNotNull();

        // external task is still not resolved
        final List<ExternalTask> externalTasks4 = externalTaskService.createExternalTaskQuery().processInstanceId(
                processInstance.getId()).list();
        assertThat(externalTasks4).isNotNull();
        assertThat(externalTasks4.size()).isEqualTo(1);
        // Exception aborted processing so retries could not be set!
        assertThat(externalTasks4.get(0).getRetries()).isEqualTo(2);
        assertThat(externalTasks4.get(0).getErrorMessage()).isEqualTo(DONTCHANGEMSG);

        // assert that process not in the end event "HappyEnd"
        assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(
                processInstance.getId()).activityId("HappyEnd").singleResult()).isNull();

        // assert that process ended not due to error boundary event 4711
        assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(
                processInstance.getId()).activityId("End4711").singleResult()).isNull();

        // assert that process ended not due to error boundary event 0815
        assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(
                processInstance.getId()).activityId("End0815").singleResult()).isNull();

        // assert that the variables unchanged
        final List<HistoricVariableInstance> variables2 = historyService.createHistoricVariableInstanceQuery().processInstanceId(
                processInstance.getId()).list();
        assertThat(variables2.size()).isEqualTo(3);
        final HashMap<String, Object> variablesAsMap2 = new HashMap<String, Object>();
        for (final HistoricVariableInstance variable : variables2) {
            variablesAsMap2.put(variable.getName(), variable.getValue());
        }
        assertThat(variablesAsMap2.containsKey("var1")).isTrue();
        assertThat(variablesAsMap2.get("var1")).isEqualTo("foo");
        assertThat(variablesAsMap2.containsKey("var2")).isTrue();
        assertThat(variablesAsMap2.get("var2")).isEqualTo("bar");
        assertThat(variablesAsMap2.containsKey("var3")).isTrue();
        assertThat(variablesAsMap2.get("var3")).isEqualTo("foobar");

        /*
         * DontChangeRetriesException
         */

        mockEndpoint.reset();

        final String CREATEINCIDENT = "Incident";

        // variables returned but must not be set since task will not be
        // completed
        mockEndpoint.whenAnyExchangeReceived(new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                throw new CreateIncidentException(CREATEINCIDENT);
            }
        });

        // call route "direct:testRoute"
        final ProducerTemplate template3 = camelContext.createProducerTemplate();
        try {
            template3.requestBodyAndHeader("direct:firstTestRoute",
                    null,
                    "CamundaBpmExternalTaskId",
                    lockedExternalTask.getId());
            Assert.fail("Expected an exception, but Camel route succeeded!");
        } catch (Exception e) {
            // expected
        }

        // ensure endpoint has been called
        assertThat(mockEndpoint.assertExchangeReceived(0)).isNotNull();

        // external task is still not resolved
        final List<ExternalTask> externalTasks3 = externalTaskService.createExternalTaskQuery().processInstanceId(
                processInstance.getId()).list();
        assertThat(externalTasks3).isNotNull();
        assertThat(externalTasks3.size()).isEqualTo(1);
        // Exception aborted processing so retries could not be set!
        assertThat(externalTasks3.get(0).getRetries()).isEqualTo(0);
        assertThat(externalTasks3.get(0).getErrorMessage()).isEqualTo(CREATEINCIDENT);

        // assert that process not in the end event "HappyEnd"
        assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(
                processInstance.getId()).activityId("HappyEnd").singleResult()).isNull();

        // assert that process ended not due to error boundary event 4711
        assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(
                processInstance.getId()).activityId("End4711").singleResult()).isNull();

        // assert that process ended not due to error boundary event 0815
        assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(
                processInstance.getId()).activityId("End0815").singleResult()).isNull();

        // assert that the variables unchanged
        final List<HistoricVariableInstance> variables3 = historyService.createHistoricVariableInstanceQuery().processInstanceId(
                processInstance.getId()).list();
        assertThat(variables3.size()).isEqualTo(3);
        final HashMap<String, Object> variablesAsMap3 = new HashMap<String, Object>();
        for (final HistoricVariableInstance variable : variables3) {
            variablesAsMap3.put(variable.getName(), variable.getValue());
        }
        assertThat(variablesAsMap3.containsKey("var1")).isTrue();
        assertThat(variablesAsMap3.get("var1")).isEqualTo("foo");
        assertThat(variablesAsMap3.containsKey("var2")).isTrue();
        assertThat(variablesAsMap3.get("var2")).isEqualTo("bar");
        assertThat(variablesAsMap3.containsKey("var3")).isTrue();
        assertThat(variablesAsMap3.get("var3")).isEqualTo("foobar");

        // complete task to make test order not relevant
        externalTaskService.complete(externalTasks1.get(0).getId(), "0815");

    }

    @SuppressWarnings("unchecked")
    @Test
    @Deployment(resources = { "process/StartExternalTask2.bpmn20.xml" })
    public void testAsyncIsTrueAndLockDuration() throws Exception {

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
        assertThat(mockEndpoint.assertExchangeReceived(0).getProperty(EXCHANGE_HEADER_PROCESS_INSTANCE_ID)).isEqualTo(
                processInstance.getId());
        assertThat(mockEndpoint.assertExchangeReceived(1).getProperty(EXCHANGE_HEADER_PROCESS_INSTANCE_ID)).isEqualTo(
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
        assertThat(mockEndpoint.assertExchangeReceived(0).getProperty(EXCHANGE_HEADER_PROCESS_INSTANCE_ID)).isEqualTo(
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
        assertThat(mockEndpoint.assertExchangeReceived(0).getProperty(EXCHANGE_HEADER_PROCESS_INSTANCE_ID)).isEqualTo(
                processInstance.getId());
        // assert that in-headers are set properly
        assertThat(mockEndpoint.assertExchangeReceived(0).getIn().getHeader(EXCHANGE_HEADER_ATTEMPTSSTARTED)).isEqualTo(
                0);
        assertThat(mockEndpoint.assertExchangeReceived(0).getIn().getHeader(EXCHANGE_HEADER_RETRIESLEFT)).isEqualTo(2);
        assertThat(mockEndpoint.assertExchangeReceived(1).getIn().getHeader(EXCHANGE_HEADER_ATTEMPTSSTARTED)).isEqualTo(
                1);
        assertThat(mockEndpoint.assertExchangeReceived(1).getIn().getHeader(EXCHANGE_HEADER_RETRIESLEFT)).isEqualTo(1);

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
