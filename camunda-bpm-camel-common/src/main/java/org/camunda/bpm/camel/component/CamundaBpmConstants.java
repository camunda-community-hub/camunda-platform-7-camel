package org.camunda.bpm.camel.component;

/**
 * Common constants for the camunda BPM Apache Camel component
 */
public final class CamundaBpmConstants {

  public static final String CAMUNDA_BPM_CAMEL_URI_SCHEME = "camunda-bpm";
  public static final String CAMUNDA_BPM_PROCESS_DEFINITION_KEY = "CamundaBpmProcessDefinitionKey";
  public static final String CAMUNDA_BPM_PROCESS_DEFINITION_ID = "CamundaBpmProcessDefinitionId";
  public static final String CAMUNDA_BPM_PROCESS_INSTANCE_ID = "CamundaBpmProcessInstanceId";

  /* Apache Camel URI parameters */
  public final static String PROCESS_DEFINITION_KEY_PARAMETER = "processDefinitionKey";
  public final static String ACTIVITY_ID_PARAMETER = "activityId";
  public final static String COPY_MESSAGE_PROPERTIES_PARAMETER = "copyProperties";
  public final static String COPY_MESSAGE_HEADERS_PARAMETER = "copyHeaders";
  public final static String COPY_MESSAGE_BODY_AS_PROCESS_VARIABLE_PARAMETER = "copyBodyAsVariable";

  private CamundaBpmConstants() {} // prevent instantiation of helper class

  public static String camundaBpmUri(String path) {
    return CAMUNDA_BPM_CAMEL_URI_SCHEME + ":" + path;
  }
}
