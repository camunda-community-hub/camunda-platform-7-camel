package org.camunda.bpm.camel.component;

/**
 * Common constants for the camunda BPM Apache Camel component
 */
public final class CamundaBpmConstants {

    public static final String CAMUNDA_BPM_CAMEL_URI_SCHEME = "camunda-bpm";
    public static final String CAMUNDA_BPM_PROCESS_DEFINITION_KEY = "CamundaBpmProcessDefinitionKey";
    public static final String CAMUNDA_BPM_PROCESS_DEFINITION_ID = "CamundaBpmProcessDefinitionId";
    public static final String CAMUNDA_BPM_PROCESS_INSTANCE_ID = "CamundaBpmProcessInstanceId";
    public static final String CAMUNDA_BPM_PROCESS_PRIO = "CamundaBpmProcessInstancePrio";
    public static final String CAMUNDA_BPM_BUSINESS_KEY = "CamundaBpmBusinessKey";
    public static final String CAMUNDA_BPM_CORRELATION_KEY = "CamundaBpmCorrelationKey";

    /* Apache Camel URI parameters */
    public final static String PROCESS_DEFINITION_KEY_PARAMETER = "processDefinitionKey";
    public final static String TOPIC_PARAMETER = "topic";
    public final static String VARIABLESTOFETCH_PARAMETER = "variablesToFetch";
    public final static String MAXTASKSPERPOLL_PARAMETER = "maxTasksPerPoll";
    public final static int MAXTASKSPERPOLL_DEFAULT = 5;
    public final static String COMPLETETASK_PARAMETER = "completeTask";
    public final static boolean COMPLETETASK_DEFAULT = true;
    public final static String LOCKDURATION_PARAMETER = "lockDuration";
    public final static long LOCKDURATION_DEFAULT = 60000;
    public final static String RETRYTIMEOUT_PARAMETER = "retryTimeout";
    public final static String MESSAGE_NAME_PARAMETER = "messageName";
    public final static String CORRELATION_KEY_NAME_PARAMETER = "correlationKeyName";
    public final static String ACTIVITY_ID_PARAMETER = "activityId";
    public final static String COPY_MESSAGE_PROPERTIES_PARAMETER = "copyProperties";
    public final static String COPY_MESSAGE_HEADERS_PARAMETER = "copyHeaders";
    public final static String COPY_MESSAGE_BODY_AS_PROCESS_VARIABLE_PARAMETER = "copyBodyAsVariable";

    private CamundaBpmConstants() {
    } // prevent instantiation of helper class

    public static String camundaBpmUri(String path) {
        return CAMUNDA_BPM_CAMEL_URI_SCHEME + ":" + path;
    }
}
