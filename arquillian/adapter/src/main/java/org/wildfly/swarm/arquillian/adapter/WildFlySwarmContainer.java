/**
 * Copyright 2015-2016 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.swarm.arquillian.adapter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
//import java.io.File;

import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.spi.context.ContainerContext;
import org.jboss.arquillian.container.spi.context.DeploymentContext;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;
import org.wildfly.swarm.arquillian.StartupTimeout;
import org.wildfly.swarm.arquillian.daemon.container.DaemonContainerConfigurationBase;
import org.wildfly.swarm.arquillian.daemon.container.DaemonDeployableContainerBase;
//import org.wildfly.swarm.bootstrap.util.TempFileManager;

/**
 * @author Bob McWhirter
 * @author Toby Crawley
 */
public class WildFlySwarmContainer extends DaemonDeployableContainerBase<DaemonContainerConfigurationBase> {

    @Inject
    Instance<ContainerContext> containerContext;

    @Inject
    Instance<DeploymentContext> deploymentContext;


    @Override
    public Class<DaemonContainerConfigurationBase> getConfigurationClass() {
        return DaemonContainerConfigurationBase.class;
    }

    @Override
    public void start() throws LifecycleException {
        //disable start, since we call super.start() at deploy time
    }

    public void setTestClass(Class<?> testClass) {
        this.testClass = testClass;
    }

    public void setRequestedMavenArtifacts(List<String> artifacts) {
        this.requestedMavenArtifacts = new HashSet<>(artifacts);
    }

    @Override
    public ProtocolMetaData deploy(Archive<?> archive) throws DeploymentException {
        StartupTimeout startupTimeout = this.testClass.getAnnotation(StartupTimeout.class);
        if (startupTimeout != null) {
            setTimeout(startupTimeout.value());
        }

        System.out.println("--------START WILDFLYSWARM CONTAINER");

        this.delegateContainer = new UberjarSimpleContainer(this.containerContext.get(), this.deploymentContext.get(), this.testClass);

        try {
            String javaVMArguments = this.getJavaVmArguments();
            /*processFile = File.createTempFile("swarmprocessfile", null);
            processFile.deleteOnExit();
            System.out.println("----CREATED processFile:" + processFile.getAbsolutePath());
            if (javaVMArguments == null) {
                javaVMArguments = "";
            }

            javaVMArguments = javaVMArguments + " -Dorg.wildfly.swarm.container.processFile=" + processFile.getAbsolutePath();
            System.out.println("JAVAVM ARGUMENTS:" + javaVMArguments);
            System.setProperty("org.wildfly.swarm.container.processFile", processFile.getAbsolutePath());*/
            this.delegateContainer
                    .setJavaVmArguments(javaVMArguments)
                    .requestedMavenArtifacts(this.requestedMavenArtifacts)
                    .start(archive);
            // start wants to connect to the remote container, which isn't up until now, so
            // we override start above and call it here instead
            super.start();

            ProtocolMetaData metaData = new ProtocolMetaData();
            metaData.addContext(createDeploymentContext(archive.getId()));

            return metaData;
        } catch (Exception e) {
            throw new DeploymentException(e.getMessage(), e);
        }
    }

    @Override
    public void undeploy(Archive<?> archive) throws DeploymentException {
        try {
            System.out.println("--------STOP WILDFLYSWARM CONTAINER");
            //processFile.delete();
            //super.stop();
            this.delegateContainer.stop();
            //TempFileManager.INSTANCE.close();
            System.out.println("--------STOPPED WILDFLYSWARM CONTAINER");
        } catch (Exception e) {
            throw new DeploymentException("Unable to stop process", e);
        }
    }

    @Override
    public void deploy(Descriptor descriptor) throws DeploymentException {
    }

    @Override
    public void undeploy(Descriptor descriptor) throws DeploymentException {
    }

    private Set<String> requestedMavenArtifacts = new HashSet<>();

    private SimpleContainer delegateContainer;

    private Class<?> testClass;

    //private File processFile;
}
