This component uses OSGi and blueprint to interact with Camel. It uses the OSGi Framework to retrieve the ‘ProcessEngine’ and a ‘DefaultCamelContext’ therefore the bean definition of the ‘CamelServiceImpl’ is obsolete.
The camunda-bpm-osgi project is used with the blueprint-wrapper ‘context.xml’. The ‘BlueprintELResolver’ was extended by the ‘CamelBlueprintELResolver’. You need to replace the class of the ‘blueprintELResolver’ bean in the context.xml:
‘…
<bean id="blueprintELResolver" class=" org.camunda.bpm.camel.blueprint.CamelBlueprintELResolver" />
..’
