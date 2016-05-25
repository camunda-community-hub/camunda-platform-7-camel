package org.camunda.bpm.camel.component.consumer;

import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.camel.Endpoint;
import org.apache.camel.Processor;
import org.camunda.bpm.camel.common.UriUtils;
import org.camunda.bpm.camel.component.CamundaBpmEndpointDefaultImpl;

public class CamundaBpmConsumerFactory {

	private CamundaBpmConsumerFactory() {
	}

    public static CamundaBpmConsumer createConsumer(Endpoint endpoint, String uri, Processor processor, ScheduledExecutorService executor, Map<String, Object> parameters) {
		String[] uriTokens = UriUtils.parseUri(uri);

		if (uriTokens.length > 0) {
			if ("externalTask".equals(uriTokens[0])) {
				return new ExternalTaskConsumer(endpoint, processor, executor, parameters);
			}
		}

		throw new IllegalArgumentException("Cannot create a consumer for URI '"
				+ uri);
    }

	public static CamundaBpmConsumer createConsumer(
			CamundaBpmEndpointDefaultImpl endpoint, String uri, Processor processor,
			Map<String, Object> parameters) throws IllegalArgumentException {
		String[] uriTokens = UriUtils.parseUri(uri);

		if (uriTokens.length > 0) {
			if ("externalTask".equals(uriTokens[0])) {
				return new ExternalTaskConsumer(endpoint, processor, parameters);
			}
		}

		throw new IllegalArgumentException("Cannot create a consumer for URI '"
				+ uri);
	}

}
