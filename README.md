![camunda BPM + Apache Camel][1]

This project aims at bringing two great BPM and EIP Open Source frameworks closer together, the [camunda BPM platform](http://camunda.org) and [Apache Camel](http://camel.camunda.org) in order to bring the development of Process-driven  applications to a whole new level.

This project started as a fork of [Activiti's Apache Camel module](https://github.com/Activiti/Activiti/tree/master/modules/activiti-camel) and we have taken a back-to-basics approach in the migration of the code to camunda BPM. Instead of migrating all the exisiting Activiti features as-is, we have decided to start with the bare minimum that adds value and iterate/refactor from that.

# Great! How do I use it in my project?!

## Add the library to your `pom.xml'

Declare the camunda BPM repository in your project's `pom.xml` and make sure you also add the `<updatePolicy>` element so Maven always downloads the latest SNAPSHOT: 

```xml
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
...
```

Choose a dependency depending on your target environment:

### Spring
```xml
<dependency>
    <groupId>org.camunda.bpm.incubation</groupId>
    <artifactId>camunda-bpm-camel-spring</artifactId>
    <version>0.1-SNAPSHOT</version>
</dependency>
```

### CDI
```xml
<dependency>
    <groupId>org.camunda.bpm.incubation</groupId>
    <artifactId>camunda-bpm-camel-spring</artifactId>
    <version>0.1-SNAPSHOT</version>
</dependency>
```

## Container Configuration

## Spring Framework

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

## CDI

The CDI configuration needs a bit more work. Make sure you read [Apache Camel's CDI documentation](http://camel.apache.org/cdi.html). Then have a look at the CDI integration tests:

* [ArquillianTestsProcessApplication](https://github.com/camunda/camunda-bpm-camel/blob/master/camunda-bpm-camel-cdi/src/test/java/org/camunda/bpm/camel/cdi/ArquillianTestsProcessApplication.java): this class is [bootstrapped by the camunda BPM platform](http://docs.camunda.org/guides/user-guide/#the-ejb-process-application) and we use it to have a guaranteed way of bootstrapping the Camel CDI context and the Camel routes. We need this because the [@Startup annotation is (unfortunately) lazily initializied](http://rmannibucau.wordpress.com/2012/12/11/ensure-some-applicationscoped-beans-are-eagerly-initialized-with-javaee/). If you know a better way to do this, just [shout](https://groups.google.com/forum/?fromgroups#!forum/camunda-bpm-dev)!
* [CamelContextBootstrap](https://github.com/camunda/camunda-bpm-camel/blob/master/camunda-bpm-camel-cdi/src/test/java/org/camunda/bpm/camel/cdi/CamelContextBootstrap.java): gets the  CdiCamelContext injected, registers the camunda BPM Camel component, registers the routes and starts the Camel context.
* [BaseArquillianIntegrationTest](https://github.com/camunda/camunda-bpm-camel/blob/master/camunda-bpm-camel-cdi/src/test/java/org/camunda/bpm/camel/cdi/BaseArquillianIntegrationTest.java): base class that all Arquillian tests use. Have a look at this class for the maven dependencies you will need for your app.

# Usage Patterns

The following usage patterns are already supported in both Spring and CDI environments and we provide a (working) test for each. Please note that you will need a [camunda BPM JBoss AS distribution](http://camunda.org/download/) up and running to be able to execute the Arquillian CDI tests. Once you have that, use `mvn verify` to execute them.

## Start a process instance from a Camel route

Send a message to the Camel endpoint `camunda-bpm:<process definition>`. The property `CamundaBpmProcessInstanceId` will be available to the downstream processors in the Camel route. 

Environment | Test
--- | --- 
Spring | [StartProcessFromRouteTest](https://github.com/rafacm/camunda-bpm-camel/blob/master/camunda-bpm-camel-spring/src/test/java/org/camunda/bpm/camel/spring/StartProcessFromRouteTest.java) 
CDI | [StartProcessFromRouteIT](https://github.com/camunda/camunda-bpm-camel/blob/master/camunda-bpm-camel-cdi/src/test/java/org/camunda/bpm/camel/cdi/StartProcessFromRouteIT.java)


## Send data to a Camel endpoint

Create a ServiceTask with the following expression `${camel.sendTo(execution, <camel endpoint URI>, '<process variable for body of message>')}`. The property `CamundaBpmProcessInstanceId` will be available to any downstream processesors in the route.

Environment | Test
--- | --- 
Spring | [SendToCamelTest](https://github.com/rafacm/camunda-bpm-camel/blob/master/camunda-bpm-camel-spring/src/test/java/org/camunda/bpm/camel/spring/SendToCamelTest.java)
CDI | [SendToCamelIT](https://github.com/camunda/camunda-bpm-camel/blob/master/camunda-bpm-camel-cdi/src/test/java/org/camunda/bpm/camel/cdi/SendToCamelIT.java)


## Signal a process instance waiting at a receive task

Create a ReceiveTask in your BPMN model and send a message in Camel to the following endpoint `camunda-bpm:<process definition id>:<receive task id>`. Note that the property `CamundaBpmProcessInstanceId` needs to be present in the message in order to be able to correlate the signal to the appropriate `ReceiveTask`.

Environment | Test
--- | --- 
Spring | [ReceiveFromCamelTest](https://github.com/rafacm/camunda-bpm-camel/blob/master/camunda-bpm-camel-spring/src/test/java/org/camunda/bpm/camel/spring/ReceiveFromCamelTest.java)
CDI | [ReceiveFromCamelIT](https://github.com/camunda/camunda-bpm-camel/blob/master/camunda-bpm-camel-cdi/src/test/java/org/camunda/bpm/camel/cdi/ReceiveFromCamelIT.java)

# Development

This project is part of the [camunda BPM incubation space](https://github.com/camunda/camunda-bpm-incubation) and still needs some work to bring it up to version 1.0.

Brutal honest (and constructive) feedback, pull requests, ... you name it... are very welcome! Meet us on the [camunda BPM dev list](https://groups.google.com/forum/?fromgroups#!forum/camunda-bpm-dev) list.

## TODOs

- [x] initial CDI support
- [ ] Exception handling, i.e. Apache Camel exceptions to BPMNErrors mapping
- [ ] Implement asynchronous support  
- [ ] Start process by process definition key passed in message property.
- [ ] Copy all other process variables as message properties when sending to a Camel endpoint
- [ ] Better data mapping (process variables <-> Camel) configuration
- [ ] Refactor Camel to camunda BPM signalling code to use the [Activity Instance Model](http://camundabpm.blogspot.de/2013/06/introducing-activity-instance-model-to.html) and not process instance IDs or execution IDs
- [ ] Deploy process definition from Camel message

## Project Structure

Since one of the (many unique) strong points of camunda BPM is that it supports (well) both the Spring and CDI environments, this projects is split into several submodules to catter for each of them:

* **camunda-bpm-camel-common**: common code shared between both Spring and CDI modules
* **camunda-bpm-camel-common-tests**: common test resources (mainly BPMN process definition files)
* **camunda-bpm-camel-spring**: Spring Framework support
* **camunda-bpm-camel-cdi**: JavaEE/CDI support

# Credits

This library started as a fork of [Activiti's Apache Camel module](https://github.com/Activiti/Activiti/tree/master/modules/activiti-camel) and the following people have contributed to its further develoment in the context of camunda BPM:

* [Rafael Cordones](http://rafael.cordones.me/)
* [Bernd Rücker](http://camunda.org/community/team.html)

# License

This software is licensed under the terms you  find in the file named `LICENSE.txt` in the root directory.

[1]: http://rafael.cordones.me/assets/camunda-bpm-camel.png
