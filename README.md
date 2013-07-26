![camunda BPM + Apache Camel][1]

This project aims at bringing to great BPM and EIP Open Source frameworks closer together, the [camunda BPM platform](http://camunda.org) and [Apache Camel](http://camel.camunda.org). This way, we believe we can bring the development of Process-driven process applications a whole new level of awesomeness.

# Approach

This library started as a fork of [Activiti's Apache Camel module](https://github.com/Activiti/Activiti/tree/master/modules/activiti-camel) and we have taken a back-to-basics approach in the migration of the code to camunda BPM. What this basically means is that 

# Project Structure

Since one of the (many unique) strong points of camunda BPM is that it supports (well) both the Spring and CDI environments, this projects is split into several submodules to catter for each of them:

* **camunda-bpm-camel-common**: common code shared between both Spring and CDI modules
* **camunda-bpm-camel-common-tests**: common test resorces (mainly BPMN process definition files)
* **camunda-bpm-camel-spring**: Spring Framework support
* **camunda-bpm-camel-cdi**: JavaEE/CDI support

# What works?

The **Spring module** already supports the following use cases:

## Start a process instance from a Camel route

Send a message to the Camel endpoint `camunda-bpm:<process definition>`. The property `CamundaBpmProcessInstanceId` will be available to the downstream processesors in the Camel route. 

Check the test [StartProcessFromRouteTest](https://github.com/rafacm/camunda-bpm-camel/blob/master/camunda-bpm-camel-spring/src/test/java/org/camunda/bpm/camel/spring/StartProcessFromRouteTest.java) and it's Spring configuration in [start-process-from-route-config.xml](hhttps://github.com/rafacm/camunda-bpm-camel/blob/master/camunda-bpm-camel-spring/src/test/resources/start-process-from-route-config.xml).

## Send data to a Camel endpoint

Create a ServiceTask with the following expression `${camel.sendTo(execution, <camel endpoint URI>, <process variable for body of message>)}`. The property `CamundaBpmProcessInstanceId` will be available to the downstream processesors in the route.

Check the test [SendToCamelTest](https://github.com/rafacm/camunda-bpm-camel/blob/master/camunda-bpm-camel-spring/src/test/java/org/camunda/bpm/camel/spring/SendToCamelTest.java) and it's Spring configuration in [send-to-camel-config.xml](https://github.com/rafacm/camunda-bpm-camel/blob/master/camunda-bpm-camel-spring/src/test/test/resources/send-to-camel-config.xml).

## Signal a process instance waiting at a receive task

Create a ReceiveTask in your BPMN model and send a message in Camel to the following ID `camunda-bpm:<process definition id>:<receive task id>`. Note that the property `CamundaBpmProcessInstanceId` needs to be present in the message in order to be able to correlate the signal to the appropriate `ReceiveTask`.

Check the test [ReceiveFromCamelTest](https://github.com/rafacm/camunda-bpm-camel/blob/master/camunda-bpm-camel-spring/src/test/java/org/camunda/bpm/camel/spring/ReceiveFromCamelTest.java) and it's Spring configuration in [receive-from-camel-config.xml](https://github.com/rafacm/camunda-bpm-camel/blob/master/camunda-bpm-camel-spring/src/test/resources/receive-from-camel-config.xml).

# Spring Configuration

In your Spring configuration you need to configure the `CamelService` like this:
```xml
...
  <bean id="camel" class="org.camunda.bpm.camel.spring.impl.CamelServiceImpl">
    <property name="processEngine" ref="processEngine"/>
    <property name="camelContext" ref="camelContext"/>
  </bean>
...
```
The Spring bean id `camel` will be then available to expressions used in ServiceTasks to send data to Camel.

# What doesn't work?

The CDI module is at the moment a **hazardous working area** and should only be entered with the appropriate protection! Follow up on the [camunda BPM dev list](https://groups.google.com/forum/?fromgroups#!forum/camunda-bpm-dev) for more on the further development of this module.

# Credits

This library started as a fork of [Activiti's Apache Camel module](https://github.com/Activiti/Activiti/tree/master/modules/activiti-camel) and the following people have contributed to its further develoment in the context of camunda BPM:
* [Rafael Cordones](http://rafael.cordones.me/)
* [Bernd Rücker](http://camunda.org/community/team.html)

# TODOs

* Start process by process definition key passed in message property.
* Copy all the other process variables as message properties
* What happens when we have more than one receive task active for a given process instance, i.e. several waiting process executions?
* Support for CDI
* Exception handling
* Better Apache Camel exceptions to BPMNErrors mapping
* Better data mapping (process variables <-> Camel) configuration
* Deploy process definition from Camel message

# Feedback

Suggestions, pull requests, ... you name it... are very welcome! Meet us on the [camunda BPM dev list](https://groups.google.com/forum/?fromgroups#!forum/camunda-bpm-dev) list.

# License

This software is licensed under the terms you  find in the file named `LICENSE.txt` in the root directory.

[1]: http://rafael.cordones.me/assets/camunda-bpm-camel.png
