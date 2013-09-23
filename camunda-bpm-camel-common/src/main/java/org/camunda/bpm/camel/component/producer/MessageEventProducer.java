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

import static org.camunda.bpm.camel.component.CamundaBpmConstants.MESSAGE_NAME_PARAMETER;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.PROCESS_DEFINITION_KEY_PARAMETER;

import java.util.Map;

import org.apache.camel.Exchange;
import org.camunda.bpm.camel.common.ExchangeUtils;
import org.camunda.bpm.camel.component.CamundaBpmEndpoint;
import org.camunda.bpm.engine.runtime.Execution;

/**
 * Signals a process instance given a process definition key.
 *
 * Example: camunda-bpm://message?messageName=messageName&activityId=anActivityId
 *
 * @author Rafael Cordones (@rafacm)
 * @author Bernd Ruecker
 */
public class MessageEventProducer extends CamundaBpmProducer {

  private final String messageName;
  private final String processDefinitionKey;

  public MessageEventProducer(CamundaBpmEndpoint endpoint, Map<String, Object> parameters) {
    super(endpoint, parameters);


    if (parameters.containsKey(MESSAGE_NAME_PARAMETER)) {
      this.messageName = (String) parameters.get(MESSAGE_NAME_PARAMETER);
    } else { 
      throw new IllegalArgumentException("You need to pass the '" + MESSAGE_NAME_PARAMETER + "' parameter! Parameters received: " + parameters);
    }
    if (parameters.containsKey(PROCESS_DEFINITION_KEY_PARAMETER)) {
      this.processDefinitionKey = (String) parameters.get(PROCESS_DEFINITION_KEY_PARAMETER);
    }
    else {
      this.processDefinitionKey = null;
    }
  }

  @Override
  public void process(Exchange exchange) throws Exception {
    String processInstanceId = findProcessInstanceId(exchange, processDefinitionKey);
    Execution execution = runtimeService.createExecutionQuery()
                                        .processDefinitionKey(processDefinitionKey)
                                        .processInstanceId(processInstanceId)
                                        .messageEventSubscriptionName(messageName).singleResult();

    if (execution == null) {
      throw new RuntimeException("Couldn't find waiting process instance with id '" + processInstanceId + "' for message '" + messageName + "'");
    }

    runtimeService.setVariables(execution.getId(), ExchangeUtils.prepareVariables(exchange, getCamundaBpmEndpoint()));
    runtimeService.messageEventReceived(messageName, execution.getId());
  }

}
