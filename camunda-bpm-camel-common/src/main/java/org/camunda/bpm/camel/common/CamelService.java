package org.camunda.bpm.camel.common;

/**
 * Service to integrate with Apache Camel.
 *
 * @author Rafael Cordones
 */
public interface CamelService {

  /**
   * Sends all process instance variables as a map to a Camel
   * {@link org.apache.camel.Endpoint}
   *
   * Example usage in a ServiceTask expression:
   *
   * ${ camel.sendTo('direct:tweets') }
   *
   * @param endpointUri
   *          an Camel {@link org.apache.camel.Endpoint} URI
   *
   * @exception Exception BpmnError: Raises business error in workflow allowing boundary error handling
   *                      Any other cheched or unchecked exception raises technical error stopping workflow at service task
   * 
   * @return the result of the execution of the Camel route
   */
  public Object sendTo(String endpointUri) throws Exception;

  /**
   * Sends the specified process instance variables as a map to an Camel
   * {@link org.apache.camel.Endpoint}
   *
   * Example usage in a ServiceTask expression:
   *
   * ${ camel.sendTo('direct:tweets', 'var1, var2') }
   *
   * @param endpointUri
   *          an Camel {@link org.apache.camel.Endpoint} URI
   * @param processVariables
   *          list of process variable names. Empty string sends no variables,
   *          null value sends all
   *
   * @exception Exception BpmnError: Raises business error in workflow allowing boundary error handling
   *                      Any other cheched or unchecked exception raises technical error stopping workflow at service task
   * 
   * @return the result of the execution of the Camel route
   */
  public Object sendTo(String endpointUri, String processVariables) throws Exception;

  /**
   * Sends the specified process instance variables as a map to an Camel
   * {@link org.apache.camel.Endpoint} and provide correlationId for callback
   *
   * Example usage in a ServiceTask expression:
   *
   * ${ camel.sendTo('direct:tweets', 'var1, var2', 'corr4711') }
   *
   * @param endpointUri
   *          an Camel {@link org.apache.camel.Endpoint} URI
   * @param processVariables
   *          list of process variable names. Empty string sends no variables,
   *          null value sends all
   * @param correlationKey
   *          value for correlation of Response. Route for response must contain
   *          a parameter correlationKeyName with the name of the process
   *          variable which is used for correlation
   * 
   * @exception Exception BpmnError: Raises business error in workflow allowing boundary error handling
   *                      Any other cheched or unchecked exception raises technical error stopping workflow at service task
   *
   * @return the result of the execution of the Camel route
   */
  public Object sendTo(String endpointUri, String processVariables,
      String correlationKey) throws Exception;
}
