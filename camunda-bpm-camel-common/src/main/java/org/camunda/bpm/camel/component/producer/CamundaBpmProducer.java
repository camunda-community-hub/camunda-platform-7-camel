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

import org.camunda.bpm.camel.common.CamundaBpmEndpoint;
import org.camunda.bpm.camel.common.ExchangeUtils;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.Execution;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultProducer;
import static org.camunda.bpm.camel.common.CamundaBpmConstants.*;

public class CamundaBpmProducer extends DefaultProducer {

  private RuntimeService runtimeService;

  private String processKey = null;

  private String activity = null;

  public CamundaBpmProducer(CamundaBpmEndpoint endpoint, RuntimeService runtimeService) {
    super(endpoint);
    this.runtimeService = runtimeService;
    String[] path = endpoint.getEndpointKey().split(":");
    processKey = path[1].replace("//", "");
    if (path.length > 2) {
      activity = path[2];
    }
  }

  public void process(Exchange exchange) throws Exception {
    signal(exchange);
  }

  private void signal(Exchange exchange) {
    String processInstanceId = findProcessInstanceId(exchange);
    Execution execution = runtimeService.createExecutionQuery()
        .processDefinitionKey(processKey)
        .processInstanceId(processInstanceId)
        .activityId(activity).singleResult();

    if (execution == null) {
      throw new RuntimeException("Couldn't find activity "+activity+" for processId " + processInstanceId);
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
      throw new RuntimeException("Could not find activiti with key " + processInstanceKey);
    }
    return processInstance.getId();
  }

  protected CamundaBpmEndpoint getActivitiEndpoint() {
    return (CamundaBpmEndpoint) getEndpoint();
  }
}
