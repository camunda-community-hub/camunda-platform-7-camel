package org.camunda.bpm.camel.cdi;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.DependencyResolvers;
import org.jboss.shrinkwrap.resolver.api.maven.MavenDependencyResolver;

public abstract class BaseArquillianIntegrationTest {

  public static WebArchive prepareTestDeployment(String deploymentArchiveName) {
    MavenDependencyResolver resolver = DependencyResolvers.use(MavenDependencyResolver.class)
        .loadMetadataFromPom("pom.xml");

    return ShrinkWrap.create(WebArchive.class, deploymentArchiveName)
        .addAsLibraries(resolver.artifact("org.camunda.bpm:camunda-engine-cdi").resolveAsFiles())
        .addAsLibraries(resolver.artifact("org.camunda.bpm.javaee:camunda-ejb-client").resolveAsFiles())
        .addAsLibraries(resolver.artifact("org.easytesting:fest-assert-core").resolveAsFiles())

        .addAsWebResource("META-INF/processes.xml", "WEB-INF/classes/META-INF/processes.xml")
        .addAsResource("process/SmokeTest.bpmn20.xml")
        ;
  }
}
