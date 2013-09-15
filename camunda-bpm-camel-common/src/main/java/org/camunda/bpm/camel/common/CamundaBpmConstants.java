package org.camunda.bpm.camel.common;

/**
 * Common constants for the camunda BPM Apache Camel component
 */
public final class CamundaBpmConstants {

  public static final String CAMUNDA_BPM_CAMEL_URI_SCHEME = "camunda-bpm";
  public static final String CAMUNDA_BPM_PROCESS_DEFINITION_KEY = "CamundaBpmProcessDefinitionKey";
  public static final String CAMUNDA_BPM_PROCESS_DEFINITION_ID = "CamundaBpmProcessDefinitionId";
  public static final String CAMUNDA_BPM_PROCESS_INSTANCE_ID = "CamundaBpmProcessInstanceId";

  private CamundaBpmConstants() {}

  public static String camundaBpmUri(String path) {
    return CAMUNDA_BPM_CAMEL_URI_SCHEME + ":" + path;
  }
}
