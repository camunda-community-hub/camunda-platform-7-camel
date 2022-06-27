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
import org.camunda.bpm.camel.common.CamelService;
import org.camunda.bpm.camel.component.CamundaBpmComponent;
import org.camunda.bpm.camel.component.CamundaBpmConstants;
import org.camunda.bpm.engine.ProcessEngine;
import org.springframework.context.annotation.Bean;

public class CamundaCamelSpringConfiguration {

  @Bean
  public CamundaBpmComponent camundaBpmComponent(ProcessEngine processEngine,
      CamelContext camelContext) {
    CamundaBpmComponent component = new CamundaBpmComponent(processEngine);
    camelContext.addComponent(CamundaBpmConstants.CAMUNDA_BPM_CAMEL_URI_SCHEME, component);

    return component;
  }

  @Bean
  public CamelService camelServiceBean(ProcessEngine processEngine, CamelContext camelContext) {
    CamelServiceBean service = new CamelServiceBean();
    service.setCamelContext(camelContext);
    service.setProcessEngine(processEngine);

    return service;
  }

}
