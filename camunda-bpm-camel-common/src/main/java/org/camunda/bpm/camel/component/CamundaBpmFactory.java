package org.camunda.bpm.camel.component;

import static org.camunda.bpm.camel.component.CamundaBpmConstants.CAMUNDA_BPM_CAMEL_URI_SCHEME;

import java.util.Map;
import java.util.regex.Pattern;

import org.camunda.bpm.camel.component.producer.CamundaBpmProducer;
import org.camunda.bpm.camel.component.producer.MessageEventProducer;
import org.camunda.bpm.camel.component.producer.SignalProcessProducer;
import org.camunda.bpm.camel.component.producer.StartProcessProducer;

/**
 * Creates producers according to the URI passed
 */
public final class CamundaBpmFactory {

//  private static final Logger log = LoggerFactory.getLogger(CamundaBpmFactory.class);

  private CamundaBpmFactory() { } // Prevent instantiation of helper class

  public static CamundaBpmProducer createProducer(CamundaBpmEndpoint endpoint, String uri, Map<String, Object> parameters) throws IllegalArgumentException {
    String[] uriTokens = parseUri(uri);

    if (uriTokens.length > 0) {
      if ("start".equals(uriTokens[0])) {
        return new StartProcessProducer(endpoint, parameters);
      } else if ("signal".equals(uriTokens[0])) {
        return new SignalProcessProducer(endpoint, parameters);
      } else if ("message".equals(uriTokens[0])) {
        return new MessageEventProducer(endpoint, parameters);
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
