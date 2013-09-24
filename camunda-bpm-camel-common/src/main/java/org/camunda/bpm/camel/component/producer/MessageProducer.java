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

import static org.camunda.bpm.camel.component.CamundaBpmConstants.*;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Exchange;
import org.camunda.bpm.camel.common.ExchangeUtils;
import org.camunda.bpm.camel.component.CamundaBpmEndpoint;
import org.camunda.bpm.engine.runtime.Execution;
import org.camunda.bpm.engine.runtime.ExecutionQuery;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.runtime.ProcessInstanceQuery;

/**
 * Sends a message (or signals a ReceiveTask) to a waiting process instance or
 * start a new process instance by message.
 * 
 * Example: camunda-bpm://message?messageName=someMessage
 * 
 * Example: camunda-bpm://message?activityId=receiveTask
 * 
 * @author Rafael Cordones (@rafacm)
 * @author Bernd Ruecker
 */
public class MessageProducer extends CamundaBpmProducer {

  private final String messageName;
  private final String activityId;
  private final String processDefinitionKey;

  public MessageProducer(CamundaBpmEndpoint endpoint, Map<String, Object> parameters) {
    super(endpoint, parameters);

    if (parameters.containsKey(MESSAGE_NAME_PARAMETER)) {
      this.messageName = (String) parameters.get(MESSAGE_NAME_PARAMETER);
      this.activityId = null;
    } else {
      if (parameters.containsKey(ACTIVITY_ID_PARAMETER)) {
        this.messageName = null;
        this.activityId = (String) parameters.get(ACTIVITY_ID_PARAMETER);
      } else {
        throw new IllegalArgumentException("You need to pass the '" + MESSAGE_NAME_PARAMETER + "' parameter! Parameters received: " + parameters);
      }
    }
    if (parameters.containsKey(PROCESS_DEFINITION_KEY_PARAMETER)) {
      this.processDefinitionKey = (String) parameters.get(PROCESS_DEFINITION_KEY_PARAMETER);
    } else {
      this.processDefinitionKey = null;
    }
  }

  @Override
  public void process(Exchange exchange) throws Exception {    
    String processInstanceId = exchange.getProperty(CAMUNDA_BPM_PROCESS_INSTANCE_ID, String.class);
    String businessKey = exchange.getProperty(CAMUNDA_BPM_BUSINESS_KEY, String.class);
    
    if (processInstanceId == null && businessKey!=null) {
      ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery() //
          .processInstanceBusinessKey(businessKey);
      if (processDefinitionKey!=null) {
        query.processDefinitionKey(processDefinitionKey);
      }
      ProcessInstance processInstance = query.singleResult();
      if (processInstance != null) {
        processInstanceId = processInstance.getId();
      }
    }
            
    Map<String, Object> processVariables = ExchangeUtils.prepareVariables(exchange, parameters);
    
    if (messageName!=null) {
      // if we have process instance we try to send the message to this one:
      if (processInstanceId!=null) {
        ExecutionQuery query = runtimeService.createExecutionQuery() //
          .processInstanceId(processInstanceId) //
          .messageEventSubscriptionName(messageName); 
        if (processDefinitionKey!=null) {
          query.processDefinitionKey(processDefinitionKey);
        }
        Execution execution = query.singleResult();
  
        if (execution == null) {
          throw new RuntimeException("Couldn't find waiting process instance with id '" + processInstanceId + "' for message '" + messageName + "'");
        }
  
        runtimeService.messageEventReceived(messageName, execution.getId(), processVariables);
      } else {
        // otherwise we just send the message to the engine to let the engine decide what to do
        // this can either correlate to a waiting instance or start a new process Instance
        
        HashMap<String, Object> correlationKeys = new HashMap<String, Object>();
        // TODO: How to retrieve correlation keys?
        
        // workaround for https://app.camunda.com/jira/browse/CAM-1316 - at the moment we just start process instances
        // and skip correlation!
        //runtimeService.correlateMessage(messageName, correlationKeys, processVariables);
        runtimeService.startProcessInstanceByMessage(messageName, processVariables);
      }
    }
    else {
      // signal a ReceiveTask needs a processInstance to be addressed
      if (processInstanceId==null) {
        throw new RuntimeException("Could not find the process instance via the provided properties (" + CAMUNDA_BPM_PROCESS_INSTANCE_ID + "= '" + processInstanceId + "', " + CAMUNDA_BPM_BUSINESS_KEY + "= '" + businessKey + "'");      
      }      
      
      ExecutionQuery query = runtimeService.createExecutionQuery() //
          .processInstanceId(processInstanceId) //
          .activityId(activityId); //
      if (processDefinitionKey!=null) {
        query.processDefinitionKey(processDefinitionKey);
      }
      Execution execution = query.singleResult();

      if (execution == null) {
        throw new RuntimeException("Couldn't find process instance with id '" + processInstanceId + "' waiting in activity '" + activityId + "'");
      }
      
      runtimeService.signal(execution.getId(), processVariables);      
    }
  }
}
