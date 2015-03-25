![camunda BPM + Apache Camel][1]

This project focuses on bringing two great Open Source frameworks closer together, the [camunda BPM platform](http://camunda.org) and [Apache Camel](http://camel.apache.org).

# Supported features

![Use Cases supported by camunda BPM Camel Component][2]

See [example project 'camel use cases'](https://github.com/camunda/camunda-consulting/tree/master/showcases/camel-use-cases) for code for all of the use cases shown in the above model.

[Discuss this process model on camunda share](http://camunda.org/share/#/process/f54a4ff9-4cc1-428c-829b-a4002dcdd81f) if you have questions or feedback.



## camunda BPM --> Apache Camel

### Calling a Camel Endpoint (Service)

Use the following expression in a ServiceTask to send all the process instance variables as a map to Camel endpoint:

```
${camel.sendTo('<camel endpoint>')}
```

Alternatively you can specify which process instance variables you want to send to Camel with:

```
${camel.sendTo('<camel endpoint>', '<list of process variables>')}
```

The properties `CamundaBpmProcessInstanceId`, `CamundaBpmBusinessKey` (if available) and `CamundaBpmCorrelationKey` (if set) will be available to any downstream processesors in the Camel route.



## Apache Camel --> camunda BPM
The following use cases are supported by the camunda BPM Camel component (see [Camel Components](http://camel.apache.org/components.html)).

### `camunda-bpm://start` Start a process instance

A direct consumer to start process instances.

The following URI parameters are supported:

Parameter | Description
--- | ---
`processDefinitionKey` | the [process definition key](http://docs.camunda.org/api-references/java/org/camunda/bpm/engine/RuntimeService.html) of the process to start an instance of
`copyBodyAsVariable` | name of the process variable to which the body of the Camel should be copied. Default is `camelBody`
`copyHeaders` | whether the [Camel message headers](http://camel.apache.org/header.html) should be copied as process variables
`copyProperties` | whether the [Camel exchange](http://camel.apache.org/maven/current/camel-core/apidocs/org/apache/camel/Exchange.html) properties should be copied as process variables

If the Camel message body is a map, then all the keys will be copied as process variables of the started instance.

If the property `CamundaBpmBusinessKey` is available on the incoming message then it will be associated with the started process instance and can be later followed to look it up.

The properties `CamundaBpmProcessInstanceId`, `CamundaBpmProcessDefinitionId` and `CamundaBpmBusinessKey` are available to the downstream processors in the Camel route as Camel exchange properties.

Example: `camunda-bpm://start?processDefinitionKey=startProcessFromRoute&copyBodyAsVariable=var1`

Starts a process instance of the process definition `startProcessFromRoute` with the body of the message as a map with process variable `var1` as a key.



### `camunda-bpm://message` Send a message to the process engine

A direct consumer to send a message to the process engine. This can either:
* trigger the start of a new process instance, see [Start Message Event](http://docs.camunda.org/latest/api-references/bpmn20/#events-message-events)
* send a message to a waiting process instances. The process instance might either wait in a [ReceiveTask](http://docs.camunda.org/latest/api-references/bpmn20/#tasks-receive-task) or an [Intermediate Message Event](http://docs.camunda.org/latest/api-references/bpmn20/#events-message-events).

The following URI parameters are supported:

Parameter | Description
--- | ---
`activityId`| the id of the ReceiveTask in the BPMN 2.0 XML (mandatory if the process instance waits in a ReceiveTask)
`messageName`| the name of the message in the BPMN 2.0 XML (mandatory if you do not correlate to a ReceiveTask)
`correlationKeyName`| the name of a process variable to which the property `CamundaBpmCorrelationKey` will be correlated
`copyBodyAsVariable` | name of the process variable to which the body of the Camel should be copied. Default is `camelBody`.
`processDefinitionKey` | the [process definition key](http://docs.camunda.org/api-references/java/org/camunda/bpm/engine/RuntimeService.html) of the process definition this operation is related to. Sometimes this can help to make correlation unique, it is always an optional parameter.

Note that either one of the properties `CamundaBpmProcessInstanceId`, `CamundaBpmBusinessKey` or `CamundaBpmCorrelationKey` need to be present in the message if it is correlated to a waiting process instance.




# Examples
Check the existing integration tests for guidance on how to use the current supported features in your projects: [Spring](https://github.com/rafacm/camunda-bpm-camel/blob/master/camunda-bpm-camel-spring/src/test/java/org/camunda/bpm/camel/spring) or [CDI](https://github.com/camunda/camunda-bpm-camel/blob/master/camunda-bpm-camel-cdi/src/test/java/org/camunda/bpm/camel/cdi/). To run the CDI integration tests do `mvn -DskipITs=false`.

Further there exist two example projects showing camunda-bpm-camel in Action (on JBoss AS 7 though):
* [camel use cases](https://github.com/camunda/camunda-consulting/tree/master/showcases/camel-use-cases)
* [Bank Account Opening Process using Camel](https://github.com/camunda/camunda-consulting/tree/master/showcases/bank-account-opening-camel)



# Using it in your project
This project is at the moment in incubation phase. This means that changes are bound to happen that will break backwards compatibility. Be warned!

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
    <groupId>org.camunda.bpm.extension.camel</groupId>
    <artifactId>camunda-bpm-camel-spring</artifactId>
    <version>0.1</version>
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
    <groupId>org.camunda.bpm.extension.camel</groupId>
    <artifactId>camunda-bpm-camel-cdi</artifactId>
    <version>0.1</version>
</dependency>
```

The CDI configuration needs a bit more work - especially for bootstrapping Camel. The easiest is to do this in a Singelton Startup EJB (see [Example: CamelBootStrap.java](https://github.com/camunda/camunda-bpm-examples/blob/master/camel-use-cases/src/main/java/org/camunda/demo/camel/CamelBootStrap.java)):

```
@Singleton
@Startup
public class CamelBootStrap {

  @Inject
  private CdiCamelContext cdiCamelContext;

  @Inject
  private ProcessEngine processEngine;

  @Inject
  private MyCamelRouteBuilder routeBuilder; // your own route declaration

  @PostConstruct
  public void init() throws Exception {
    CamundaBpmComponent component = new CamundaBpmComponent(processEngine);
    component.setCamelContext(cdiCamelContext);
    cdiCamelContext.addComponent("camunda-bpm", component);
    cdiCamelContext.addRoutes(routeBuilder);
    cdiCamelContext.start();
  }

  @PreDestroy
  public void shutDown() throws Exception {
    cdiCamelContext.stop();
  }
}
```

Best read [Apache Camel's CDI documentation](http://camel.apache.org/cdi.html) and have a look at the CDI integration tests [here](https://github.com/camunda/camunda-bpm-camel/blob/master/camunda-bpm-camel-cdi/src/test/java/org/camunda/bpm/camel/cdi/) for guidance.


### Blueprint
```
<dependency>
    <groupId>org.camunda.bpm.extension.camel</groupId>
    <artifactId>camunda-bpm-camel-blueprint</artifactId>
    <version>0.1</version>
</dependency>
```
The OSGi Framework is used to retrieve the `ProcessEngine` and a `DefaultCamelContext` therefore the bean definition of the `CamelServiceImpl` is obsolete.

The camunda-bpm-osgi project is used with the blueprint-wrapper `context.xml`. The `BlueprintELResolver` was extended by the `CamelBlueprintELResolver`. You need to replace the class of the ‘blueprintELResolver’ bean in the context.xml:

```
...
<bean id="blueprintELResolver" class=" org.camunda.bpm.camel.blueprint.CamelBlueprintELResolver" />
...
```


# Feedback and further development

This project is part of the [camunda BPM incubation space](https://github.com/camunda/camunda-bpm-incubation). Feedback, pull requests, ... you name it... are very welcome! Meet us on the [camunda BPM dev list](https://groups.google.com/forum/?fromgroups#!forum/camunda-bpm-dev) list.

Out landry list of development TODOs (in no special order):

- Create JBoss Distribution with Camel (including Bootstrapping) as a JBoss Module and Routes to be defined within Process Applications [CIS-19](https://app.camunda.com/jira/browse/CIS-19)
- Exception handling, i.e. Apache Camel exceptions to BPMNErrors mapping
- Implement asynchronous support
- Refactor Camel to camunda BPM signalling code to use the [Activity Instance Model](http://camundabpm.blogspot.de/2013/06/introducing-activity-instance-model-to.html) and not process instance IDs or execution IDs

These use cases are considered not interessting - tell us if you think different!
- Deploy process definition from Camel message


# Credits

This library started as a fork of [Activiti's Apache Camel module](https://github.com/Activiti/Activiti/tree/master/modules/activiti-camel) and the following people have contributed to its further develoment in the context of camunda BPM: [contributors](https://github.com/camunda/camunda-bpm-camel/graphs/contributors).

# License

This software is licensed under the terms you  find in the file named `LICENSE.txt` in the root directory.

[1]: http://rafael.cordones.me/assets/camunda-bpm-camel.png
[2]: https://raw.github.com/camunda/camunda-bpm-camel/master/use-cases.png
