package org.camunda.bpm.camel.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.CAMUNDA_BPM_CAMEL_URI_SCHEME;
import static org.camunda.bpm.camel.component.CamundaBpmConstants.camundaBpmUri;

import org.apache.camel.Component;
import org.apache.camel.ResolveEndpointFailedException;
import org.assertj.core.api.Assertions;
import org.camunda.bpm.camel.BaseCamelTest;
import org.camunda.bpm.camel.component.CamundaBpmComponent;
import org.junit.Test;

public class UriParsingTest extends BaseCamelTest {

    @Test
    public void testThatCamelContextIsInitialized() throws Exception {
        Component component = camelContext.getComponent(CAMUNDA_BPM_CAMEL_URI_SCHEME);
        assertThat(component).isInstanceOf(CamundaBpmComponent.class);
    }

    @Test(expected = ResolveEndpointFailedException.class)
    public void testGetCamundaEndpointWithUnknownUriExtension() throws Exception {
        camelContext.getEndpoint(camundaBpmUri("what/ever"));
    }
}