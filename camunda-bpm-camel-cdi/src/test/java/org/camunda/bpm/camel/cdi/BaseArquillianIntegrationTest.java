package org.camunda.bpm.camel.cdi;

import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.DependencyResolvers;
import org.jboss.shrinkwrap.resolver.api.maven.MavenDependencyResolver;

import javax.inject.Inject;

public abstract class BaseArquillianIntegrationTest {

  @Inject
  @SuppressWarnings("cdi-ambiguous-dependency")
  protected RuntimeService runtimeService;

  @Inject
  @SuppressWarnings("cdi-ambiguous-dependency")
  protected TaskService taskService;

  @Inject
  @SuppressWarnings("cdi-ambiguous-dependency")
  protected HistoryService historyService;

  protected static WebArchive prepareTestDeployment(String deploymentArchiveName, String processDefinition) {
    MavenDependencyResolver resolver = DependencyResolvers.use(MavenDependencyResolver.class)
        .loadMetadataFromPom("pom.xml");

    return ShrinkWrap.create(WebArchive.class, deploymentArchiveName)
        .addAsLibraries(resolver.artifact("org.camunda.bpm:camunda-engine-cdi").resolveAsFiles())
        .addAsLibraries(resolver.artifact("org.camunda.bpm.javaee:camunda-ejb-client").resolveAsFiles())
        .addAsLibraries(resolver.artifact("org.camunda.bpm.incubation:camunda-bpm-camel-common").resolveAsFiles())
        //.addAsLibraries(resolver.artifact("org.camunda.bpm.incubation:camunda-bpm-camel-cdi").resolveAsFiles())
        .addAsLibraries(resolver.artifact("org.apache.camel:camel-core").resolveAsFiles())
        .addAsLibraries(resolver.artifact("org.apache.camel:camel-cdi").resolveAsFiles())
        .addAsLibraries(resolver.artifact("org.easytesting:fest-assert-core").resolveAsFiles())
        .addClass(BaseArquillianIntegrationTest.class)
        .addClass(CamelContextBootstrap.class)

        .addAsWebResource("META-INF/processes.xml", "WEB-INF/classes/META-INF/processes.xml")
        .addAsResource(processDefinition)
        ;
  }
}
