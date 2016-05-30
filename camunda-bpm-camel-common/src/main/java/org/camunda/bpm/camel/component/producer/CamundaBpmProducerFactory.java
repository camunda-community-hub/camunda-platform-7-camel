package org.camunda.bpm.camel.component.producer;

import java.util.Map;

import org.camunda.bpm.camel.common.UriUtils.ParsedUri;
import org.camunda.bpm.camel.component.CamundaBpmEndpoint;

/**
 * Creates producers according to the URI passed
 */
public final class CamundaBpmProducerFactory {

    // private static final Logger log =
    // LoggerFactory.getLogger(CamundaBpmFactory.class);

    private CamundaBpmProducerFactory() {
    } // Prevent instantiation of helper class

    public static CamundaBpmProducer createProducer(final CamundaBpmEndpoint endpoint, final ParsedUri uri,
            final Map<String, Object> parameters) throws IllegalArgumentException {

        switch (uri.getType()) {
        case StartProcess:
            return new StartProcessProducer(endpoint, parameters);
        case SendSignal:
        case SendMessage:
            return new MessageProducer(endpoint, parameters);
        default:
            throw new IllegalArgumentException("Cannot create a producer for URI '" + uri + "' - new ProducerType '"
                    + uri.getType() + "' not yet supported?");
        }

    }

}
