package org.camunda.bpm.camel.component.producer;

import static org.camunda.bpm.camel.component.CamundaBpmConstants.CAMUNDA_BPM_CAMEL_URI_SCHEME;

import java.util.Map;
import java.util.regex.Pattern;

import org.camunda.bpm.camel.component.CamundaBpmEndpoint;

/**
 * Creates producers according to the URI passed
 */
public final class CamundaBpmProducerFactory {

//  private static final Logger log = LoggerFactory.getLogger(CamundaBpmFactory.class);

  private CamundaBpmProducerFactory() { } // Prevent instantiation of helper class

  public static CamundaBpmProducer createProducer(CamundaBpmEndpoint endpoint, String uri, Map<String, Object> parameters) throws IllegalArgumentException {
    String[] uriTokens = parseUri(uri);

    if (uriTokens.length > 0) {
      if ("start".equals(uriTokens[0])) {
        return new StartProcessProducer(endpoint, parameters);
      } else if ("signal".equals(uriTokens[0])) {
        return new MessageProducer(endpoint, parameters);
      } else if ("message".equals(uriTokens[0])) {
        return new MessageProducer(endpoint, parameters);
      }
    }

    throw new IllegalArgumentException("Cannot create a producer for URI '" + uri);
  }

  private static String[] parseUri(String uri) {
    Pattern p1 = Pattern.compile(CAMUNDA_BPM_CAMEL_URI_SCHEME + ":(//)*");
    Pattern p2 = Pattern.compile("\\?.*");

    uri = p1.matcher(uri).replaceAll("");
    uri = p2.matcher(uri).replaceAll("");

    return uri.split("/");
  }

}
