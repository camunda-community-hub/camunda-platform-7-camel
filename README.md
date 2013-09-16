![camunda BPM + Apache Camel][1]

This project focuses on bringing two great BPM and EIP Open Source frameworks closer together, the [camunda BPM platform](http://camunda.org) and [Apache Camel](http://camel.camunda.org) in order to bring the development of process-driven  applications to a whole new level.

Having started as a fork of [Activiti's Apache Camel module](https://github.com/Activiti/Activiti/tree/master/modules/activiti-camel), we have in the meantime refactored the code a bit.

# Current features
## camunda BPM --> Apache Camel
For the moment we provide the MVP (minimum viable possibility) to communicate with Camel, i.e. a service. In a ServiceTask you can use the following expression:

```
${camel.sendTo(execution, '<camel endpoint>', '<process variable for the message body>')}
``` 
The property `CamundaBpmProcessInstanceId` will be available to any downstream processesors in the route.

Other ways to integrate with Camel are on the pipeline (JavaDelegate, …) and will be supported soon.

## Apache Camel --> camunda BPM
The following usage cases are urrently the camunda BPM Camel component.

### `camunda-bpm://start`: Start a process instance
A direct consumer to start process instances. 

The following URI parameters are supported: 

Paremeter | Description
--- | --- 
`processDefinitionKey` | the [process definition key](http://docs.camunda.org/api-references/java/org/camunda/bpm/engine/RuntimeService.html) of the process to start an instance of 
`copyBodyAsVariable` | name of the process variable to which the body of the Camel should be copied
`copyHeaders` | whether the [Camel message headers](http://camel.apache.org/header.html) should be copied as process variables
`copyProperties` | whether the [Camel exchange](http://camel.apache.org/maven/current/camel-core/apidocs/org/apache/camel/Exchange.html) properties should be copied as process variables

The properties `CamundaBpmProcessInstanceId` and `CamundaBpmProcessDefinitionId` are available to the downstream processors in the Camel route as Camel exchange properties.

Example: `camunda-bpm://start?processDefinitionKey=startProcessFromRoute&copyBodyAsVariable=var1`
Starts a process instance of the process definition `startProcessFromRoute` 


### `camunda-bpm://signal`: Signal a process instance
A direct consumer to signal process instances.

The following URI parameters are supported:

Paremeter | Description
--- | --- 
`processDefinitionKey` | the [process definition key](http://docs.camunda.org/api-references/java/org/camunda/bpm/engine/RuntimeService.html) of the process to start an instance of 
`activityId`| the id of the ReceiveTask 
 
Note that the property `CamundaBpmProcessInstanceId` needs to be present in the message in order to be able to correlate the signal to the appropriate `ReceiveTask`.

## Examples
Check the existing integration tests for guidance on how to use the current supported features in your projects: [Spring](https://github.com/rafacm/camunda-bpm-camel/blob/master/camunda-bpm-camel-spring/src/test/java/org/camunda/bpm/camel/spring) or [CDI](https://github.com/camunda/camunda-bpm-camel/blob/master/camunda-bpm-camel-cdi/src/test/java/org/camunda/bpm/camel/cdi/).

# Using it in your project
This project is at the moment in incubation phase. This means that changes are bound to happen that will brake backwards compatibility. Be warned!

## Maven coordinates

Declare the camunda BPM repository in your project's `pom.xml` and make sure you also add the `<updatePolicy>` element so Maven always downloads the latest SNAPSHOT: 

```
<repositories>
	<repository>
		<id>camunda-bpm-nexus</id>
		<name>camunda-bpm-nexus</name>
		<url>https://app.camunda.com/nexus/content/groups/public</url>
		<snapshots>
			<updatePolicy>always</updatePolicy>
		</snapshots>
	</repository>
</repositories>
```

Choose a dependency depending on your target environment:

### Spring
```
<dependency>
    <groupId>org.camunda.bpm.incubation</groupId>
    <artifactId>camunda-bpm-camel-spring</artifactId>
    <version>0.1-SNAPSHOT</version>
</dependency>
```
In your Spring configuration you need to configure the `CamelService` like this:

```
  <bean id="camel" class="org.camunda.bpm.camel.spring.impl.CamelServiceImpl">
    <property name="processEngine" ref="processEngine"/>
    <property name="camelContext" ref="camelContext"/>
  </bean>
```

The Spring bean id `camel` will be then available to expressions used in ServiceTasks to send data to Camel.

### CDI
```
<dependency>
    <groupId>org.camunda.bpm.incubation</groupId>
    <artifactId>camunda-bpm-camel-cdi</artifactId>
    <version>0.1-SNAPSHOT</version>
</dependency>
```

The CDI configuration needs a bit more work. Make sure you read [Apache Camel's CDI documentation](http://camel.apache.org/cdi.html). Then have a look at the CDI integration tests [here](https://github.com/camunda/camunda-bpm-camel/blob/master/camunda-bpm-camel-cdi/src/test/java/org/camunda/bpm/camel/cdi/) for guidance. 

# Feedback and further development

This project is part of the [camunda BPM incubation space](https://github.com/camunda/camunda-bpm-incubation) and still needs some work to bring it up to version 1.0.

Brutal honest (and constructive) feedback, pull requests, ... you name it... are very welcome! Meet us on the [camunda BPM dev list](https://groups.google.com/forum/?fromgroups#!forum/camunda-bpm-dev) list.

Out landry list of development TODOs (in no special order):

- Exception handling, i.e. Apache Camel exceptions to BPMNErrors mapping
- Implement asynchronous support  
- Start process by process definition key passed in message property.
- Copy all other process variables as message properties when sending to a Camel endpoint
- Better data mapping (process variables <-> Camel) configuration
- Refactor Camel to camunda BPM signalling code to use the [Activity Instance Model](http://camundabpm.blogspot.de/2013/06/introducing-activity-instance-model-to.html) and not process instance IDs or execution IDs
- Deploy process definition from Camel message
- Create JBoss Distribution with Camel (including Bootstrapping) as a JBoss Module and Routes to be defined within Process Applications [CIS-19](https://app.camunda.com/jira/browse/CIS-19)

# Credits

This library started as a fork of [Activiti's Apache Camel module](https://github.com/Activiti/Activiti/tree/master/modules/activiti-camel) and the following people have contributed to its further develoment in the context of camunda BPM:

* [Rafael Cordones](http://rafael.cordones.me/)
* [Bernd Rücker](http://camunda.org/community/team.html)

# License

This software is licensed under the terms you  find in the file named `LICENSE.txt` in the root directory.

[1]: http://rafael.cordones.me/assets/camunda-bpm-camel.png
