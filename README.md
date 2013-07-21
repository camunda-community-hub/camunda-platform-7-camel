# Introduction

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

# Supported Use Cases
Our aim has been to make the mapping between camunda BPM engine and Apache Camel as explicit and transparet as possible.

## Start a process instance from a Camel route

There are two supported ways to start a process instance 

* Send a message to the Camel endpoint `camunda-bpm:<process definition>`
* Send a message to the Camel endpoint `camunda-bpm:<process definition>` containing a property called `CamundaBpmProcessDefinitionKey' with the process definition you would like to start an instance of 

In both cases above the property `CamundaBpmProcessInstanceId` will be available to the downstream processesors in the route. 

Check the test [StartProcessFromRouteTest](StartProcessFromRouteTest.java) and it's Spring configuration in [start-process-from-route-config.xml](start-process-from-route-config.xml).

## Send data to a Camel endpoint

Create a ServiceTask with the following expression `${camel.sendToEndpoint(execution, <camel endpoint URI>, <process variable for body of message>)}`. The property `CamundaBpmProcessInstanceId` will be available to the downstream processesors in the route.

TODO: copy all the other process variables as message properties.

Check the test [SendToCamelTest](SendToCamelTest) and it's Spring configuration in [send-to-camel-config.xml](send-to-camel-config.xml).

## Correlate a message to a process instance that is waiting at a receive task

TODO

## Exception handling

TODO

# Credits

This library is a fork of [Activiti's Apache Camel module](https://github.com/Activiti/Activiti/tree/master/modules/activiti-camel). 

TODO

# Roadmap

* Support for CDI
* Better Apache Camel exceptions to BPMNErrors mapping
* Better data mapping (process variables <-> Camel) configuration

# Feedback

Suggestions, pull requests, ... you name it... are very welcome! Meet us on the [camunda BPM dev list](https://groups.google.com/forum/?fromgroups#!forum/camunda-bpm-dev) list.

# License

This software is licensed under the terms you  find in the file named `LICENSE.txt` in the root directory.