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
package org.camunda.bpm.camel.component.producer;

import org.apache.camel.Exchange;
import org.camunda.bpm.camel.common.CamundaBpmEndpoint;
import org.camunda.bpm.camel.common.ExchangeUtils;
import org.camunda.bpm.engine.runtime.Execution;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import static org.camunda.bpm.camel.common.CamundaBpmConstants.*;

import java.util.Map;

/**
 * Signals a process instance given a process definition key.
 *
 * Example: camunda-bpm://signal?processDefinitionKey=aProcessDefinitionKey&activityId=anActivityId
 *
 * @author Ryan Johnston (@rjfsu)
 * @author Tijs Rademakers (@tijsrademakers)
 * @author Rafael Cordones (@rafacm)
 */
public class SignalProcessProducer extends CamundaBpmProducer {

  private final String activityId;
  private final String processDefinitionKey;

  public SignalProcessProducer(CamundaBpmEndpoint endpoint, Map<String, Object> parameters) {
    super(endpoint, parameters);

    if (parameters.containsKey(PROCESS_DEFINITION_KEY_PARAMETER) &&
        parameters.containsKey(ACTIVITY_ID_PARAMETER)) {
      this.processDefinitionKey = (String) parameters.get(PROCESS_DEFINITION_KEY_PARAMETER);
      this.activityId = (String) parameters.get(ACTIVITY_ID_PARAMETER);
    } else {
      throw new IllegalArgumentException("You need to pass the '" + PROCESS_DEFINITION_KEY_PARAMETER + "' and the '" + ACTIVITY_ID_PARAMETER + "' parameters! Parameters received: " + parameters);
    }
  }

  @Override
  public void process(Exchange exchange) throws Exception {
    String processInstanceId = findProcessInstanceId(exchange);
    Execution execution = runtimeService.createExecutionQuery()
                                        .processDefinitionKey(processDefinitionKey)
                                        .processInstanceId(processInstanceId)
                                        .activityId(activityId).singleResult();

    if (execution == null) {
      throw new RuntimeException("Couldn't find activity with id '" + activityId + "' for process instance with id '" + processInstanceId + "'");
    }
    runtimeService.setVariables(execution.getId(), ExchangeUtils.prepareVariables(exchange, getActivitiEndpoint()));
    runtimeService.signal(execution.getId());
  }

  protected String findProcessInstanceId(Exchange exchange) {
    String processInstanceId = exchange.getProperty(CAMUNDA_BPM_PROCESS_INSTANCE_ID, String.class);
    if (processInstanceId != null) {
      return processInstanceId;
    }
    String processInstanceKey = exchange.getProperty(CAMUNDA_BPM_PROCESS_DEFINITION_KEY, String.class);
    ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
      .processInstanceBusinessKey(processInstanceKey).singleResult();

    if (processInstance == null) {
      throw new RuntimeException("Could not find activity with key " + processInstanceKey);
    }
    return processInstance.getId();
  }

  protected CamundaBpmEndpoint getActivitiEndpoint() {
    return (CamundaBpmEndpoint) getEndpoint();
  }
}
