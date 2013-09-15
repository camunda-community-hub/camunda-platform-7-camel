package org.camunda.bpm.camel.common;

import org.apache.camel.Component;
import org.apache.camel.Endpoint;
import org.camunda.bpm.camel.BaseCamelTest;
import org.junit.Test;
import static org.camunda.bpm.camel.common.CamundaBpmConstants.*;
import static org.fest.assertions.api.Assertions.assertThat;

public class UriParsingTest extends BaseCamelTest {

  @Test
  public void testThatCamelContextIsInitialized() throws Exception {
    Component component = camelContext.getComponent(CAMUNDA_BPM_CAMEL_URI_SCHEME);
    assertThat(component).isInstanceOf(CamundaBpmComponent.class);
  }

  @Test
  public void testGetCamundaEndpoint() throws Exception {
    Endpoint endpoint = camelContext.getEndpoint(camundaBpmUri("what/ever"));
    assertThat(endpoint).isInstanceOf(CamundaBpmEndpoint.class);
  }
}