# Introduction

TODO

# Supported Use Cases
Our aim has been to make the mapping between camunda BPM engine and Apache Camel as explicit and transparet as possible.

## Start a process instance from a Camel route

Send a message to the Camel endpoint `camunda-bpm:<process definition>`. The property `CamundaBpmProcessInstanceId` will be available to the downstream processesors in the Camel route. 

Check the test [StartProcessFromRouteTest](https://github.com/rafacm/camunda-bpm-camel-integration/blob/master/src/test/java/org/camunda/bpm/camel/spring/StartProcessFromRouteTest.java) and it's Spring configuration in [start-process-from-route-config.xml](https://github.com/rafacm/camunda-bpm-camel-integration/blob/master/src/test/resources/start-process-from-route-config.xml).

TODO: start process by process definition key passed in message property.

## Send data to a Camel endpoint

Create a ServiceTask with the following expression `${camel.sendToEndpoint(execution, <camel endpoint URI>, <process variable for body of message>)}`. The property `CamundaBpmProcessInstanceId` will be available to the downstream processesors in the route.

TODO: copy all the other process variables as message properties.

Check the test [SendToCamelTest](https://github.com/rafacm/camunda-bpm-camel-integration/blob/master/src/test/java/org/camunda/bpm/camel/spring/SendToCamelTest.java) and it's Spring configuration in [send-to-camel-config.xml](https://github.com/rafacm/camunda-bpm-camel-integration/blob/master/src/test/resources/send-to-camel-config.xml).

## Signal a process instance waiting at a receive task

Create a ReceiveTask in your BPMN model and send a message in Camel to the following ID `camunda-bpm:<process definition id>:<receive task id>`. Note that the property `CamundaBpmProcessInstanceId` needs to be present in the message in order to be able to correlate the signal to the appropriate `ReceiveTask`.

Check the test [ReceiveFromCamelTest](https://github.com/rafacm/camunda-bpm-camel-integration/blob/master/src/test/java/org/camunda/bpm/camel/spring/ReceiveFromCamelTest.java) and it's Spring configuration in [receive-from-camel-config.xml](https://github.com/rafacm/camunda-bpm-camel-integration/blob/master/src/test/resources/receive-from-camel-config.xml).

TODO: what happens when we have more than one receive task active for a given process instance, i.e. several waiting process executions?

## Exception handling

TODO

## Asynchronous use cases

TODO

# Configuration

In your Spring configuration you need to configure the Camel service like this:
```xml
...
  <bean id="camel" class="org.camunda.bpm.camel.spring.impl.CamelServiceImpl">
    <property name="processEngine" ref="processEngine"/>
    <property name="camelContext" ref="camelContext"/>
  </bean>
...
```
The Spring bean id `camel` will be then available to expressions used in ServiceTasks to send data to Camel.

# Credits

This library is a fork of [Activiti's Apache Camel module](https://github.com/Activiti/Activiti/tree/master/modules/activiti-camel). 

TODO

# Roadmap

* Support for CDI
* Better Apache Camel exceptions to BPMNErrors mapping
* Better data mapping (process variables <-> Camel) configuration
* Deploy process definition from Camel message

# Feedback

Suggestions, pull requests, ... you name it... are very welcome! Meet us on the [camunda BPM dev list](https://groups.google.com/forum/?fromgroups#!forum/camunda-bpm-dev) list.

# License

This software is licensed under the terms you  find in the file named `LICENSE.txt` in the root directory.