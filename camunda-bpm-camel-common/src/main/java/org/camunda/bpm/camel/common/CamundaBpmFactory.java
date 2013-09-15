package org.camunda.bpm.camel.common;

import org.camunda.bpm.camel.component.producer.CamundaBpmProducer;
import org.camunda.bpm.camel.component.producer.StartProcessProducer;
import static org.camunda.bpm.camel.common.CamundaBpmConstants.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Creates producers and consumers according to the URI passed
 */
public final class CamundaBpmFactory {

  private static final Logger log = LoggerFactory.getLogger(CamundaBpmFactory.class);

  private CamundaBpmFactory() { } // Prevent instantiation of helper class

  public static CamundaBpmProducer createProducer(CamundaBpmEndpoint endpoint, String uri, Map<String, Object> parameters) throws IllegalArgumentException {
    String[] uriTokens = parseUri(uri);

    if (uriTokens.length > 0) {
      if ("start".equals(uriTokens[0])) {
        return new StartProcessProducer(endpoint, parameters);
      } else {
        return new CamundaBpmProducer(endpoint, endpoint.getProcessEngine().getRuntimeService());
      }
    } else {
      throw new IllegalArgumentException("Cannot create a producer for URI '" + uri);
    }
  }

  //public static CamundaBpmConsumer createConsumer(CamundaBpmEndpoint endpoint, String uri) throws IllegalArgumentException {
  //
  //}

  private static String[] parseUri(String uri) {
    Pattern p1 = Pattern.compile(CAMUNDA_BPM_CAMEL_URI_SCHEME + ":(//)*");
    Pattern p2 = Pattern.compile("\\?.*");

    uri = p1.matcher(uri).replaceAll("");
    uri = p2.matcher(uri).replaceAll("");

    return uri.split("/");
  }

}
