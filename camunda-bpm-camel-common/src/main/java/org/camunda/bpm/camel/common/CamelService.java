package org.camunda.bpm.camel.common;

/**
 * Service to integate with Apache Camel.
 *
 * @author Rafael Cordones <rafael@cordones.me>
 */
public interface CamelService {

  /**
   * Sends all process instance variables as a map to a Camel {@link org.apache.camel.Endpoint}
   *
   * Example usage in a ServiceTask expression:
   *
   *    ${ camel.senTo('direct:tweets') }
   *
   * @param endpointUri an Camel {@link org.apache.camel.Endpoint} URI
   *
   * @return the result of the execution of the Camel route
   */
  public Object sendTo(String endpointUri);

  /**
   * Sends the specified process instance variables as a map to an Camel {@link org.apache.camel.Endpoint}
   *
   * Example usage in a ServiceTask expression:
   *
   *    ${ camel.senTo('direct:tweets', 'var1, var2') }
   *
   * @param endpointUri an Camel {@link org.apache.camel.Endpoint} URI
   *
   * @return the result of the execution of the Camel route
   */
  public Object sendTo(String endpointUri, String processVariables);
}
