package org.camunda.bpm.camel.cdi;

import org.camunda.bpm.camel.common.CamelService;
import org.camunda.bpm.camel.spring.util.LogService;
import org.camunda.bpm.camel.spring.util.LogServiceImpl;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
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

  @Inject
  CamelContextBootstrap camelContextBootstrap;
  
  @Inject
  CamelService camelService;
  
  protected static WebArchive prepareTestDeployment(String deploymentArchiveName,
                                                    String processDefinition) {
    MavenDependencyResolver resolver = DependencyResolvers.use(MavenDependencyResolver.class)
      .loadMetadataFromPom("pom.xml");

    WebArchive war = ShrinkWrap.create(WebArchive.class, deploymentArchiveName + ".war")
      .addAsLibraries(resolver.artifact("org.camunda.bpm:camunda-engine-cdi").resolveAsFiles())
      .addAsLibraries(resolver.artifact("org.camunda.bpm.extension.camel:camunda-bpm-camel-common").resolveAsFiles())
      .addAsLibraries(resolver.artifact("org.apache.camel:camel-core").resolveAsFiles())
      .addAsLibraries(resolver.artifact("org.apache.camel:camel-cdi").resolveAsFiles())
      .addAsLibraries(resolver.artifact("org.easytesting:fest-assert-core").resolveAsFiles())
      // FIXME: this does not work we need to add this project's resources one by one
      //.addAsLibraries(resolver.artifact("org.camunda.bpm.extension.camel:camunda-bpm-camel-cdi").resolveAsFiles())
      .addClass(CamelServiceImpl.class)
      .addClass(BaseArquillianIntegrationTest.class)
      .addClass(ArquillianTestsProcessApplication.class)
      .addClass(CamelContextBootstrap.class)

      .addClass(LogService.class)
      .addClass(LogServiceImpl.class)
      .addClass(LogServiceCdiImpl.class)

      //.addClass(camelRouteClass)
      .addAsWebInfResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml"))
      .addAsWebResource("META-INF/processes.xml", "WEB-INF/classes/META-INF/processes.xml")
      .addAsResource(processDefinition)
      ;

      /*
       * For troubleshooting purposes use the following two lines to export the WAR to the filesystem
       * to see if everything needed is there!
       */
      //File destinationDir = new File("/Users/rafa/dev/plexiti/vc/the-job-announcement-fox/target");
      //war.as(ExplodedExporter.class).exportExploded(destinationDir);

    return war;
  }
}
