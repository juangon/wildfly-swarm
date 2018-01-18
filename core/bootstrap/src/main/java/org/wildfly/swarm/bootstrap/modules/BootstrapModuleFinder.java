/**
 * Copyright 2015-2017 Red Hat, Inc, and individual contributors.
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
package org.wildfly.swarm.bootstrap.modules;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.jboss.modules.DependencySpec;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.ModuleSpec;
import org.jboss.modules.ResourceLoader;
import org.jboss.modules.ResourceLoaderSpec;
import org.jboss.modules.ResourceLoaders;
import org.jboss.modules.filter.PathFilter;
import org.jboss.modules.filter.PathFilters;
import org.wildfly.swarm.bootstrap.env.ApplicationEnvironment;
import org.wildfly.swarm.bootstrap.logging.BootstrapLogger;
import org.wildfly.swarm.bootstrap.performance.Performance;
import org.wildfly.swarm.bootstrap.util.JarFileManager;
import org.wildfly.swarm.bootstrap.util.ResourceLoaderManager;
//import org.wildfly.swarm.bootstrap.util.ResourceLoaderManager;

/**
 * Module-finder used only for loading the first set of jars when run in an fat-jar scenario.
 *
 * @author Bob McWhirter
 */
public class BootstrapModuleFinder extends AbstractSingleModuleFinder {

    public static final String MODULE_NAME = "org.wildfly.swarm.bootstrap";

    public BootstrapModuleFinder() {
        super(MODULE_NAME);
    }

    @Override
    public void buildModule(ModuleSpec.Builder builder, ModuleLoader delegateLoader) throws ModuleLoadException {
        try (AutoCloseable handle = Performance.accumulate("module: Bootstrap")) {

            if (LOG.isTraceEnabled()) {
                LOG.trace("Loading module");
            }

            ApplicationEnvironment env = ApplicationEnvironment.get();

            env.bootstrapArtifactsAsCoordinates()
                    .forEach((coords) -> {
                        try {
                            File artifact = MavenResolvers.get().resolveJarArtifact(coords);
                            if (artifact == null) {
                                throw new RuntimeException("Unable to resolve artifact from coordinates: " + coords);
                            }

                            //PathFilter filter = getModuleFilter(artifact);

                            PathFilter filter = new PathFilter() {
                                @Override
                               public boolean accept(String path) {
                                    return path.endsWith("module.xml");
                                }
                            };

                            ResourceLoader originalLoader = ResourceLoaderManager.INSTANCE.addResourceLoader(artifact,
                                   () -> {
                                        try {
                                            //JarFile jar = new JarFile(artifact);
                                            JarFile jar = JarFileManager.INSTANCE.addJarFile(artifact);
                                            System.out.println("BOOTSTRAPMODULE original loader " + jar.getName());
                                            return ResourceLoaders.createJarResourceLoader(artifact.getName(), jar);
                                        } catch (IOException e) {
                                           return null;
                                        }
                                    });

                            if (originalLoader == null) {
                              throw new IOException();
                            }
                            /*JarFile jar = JarFileManager.INSTANCE.addJarFile(artifact);
                            System.out.println("BOOTSTRAPMODULE original loader " + jar.getName());
                            ResourceLoader originalLoader = ResourceLoaders.createJarResourceLoader(artifact.getName(), jar);*/

                            //ResourceLoader loader = originalLoader;
                            //ResourceLoader loader = ResourceLoaders.createFilteredResourceLoader(filter, originalLoader);
                            ResourceLoader loader = ResourceLoaderManager.INSTANCE.addResourceLoader("filtered" + artifact.getAbsolutePath(),
                                    () -> {
                                        //try {
                                           //JarFile jar = new JarFile(artifact);
                                           /*JarFile jar = JarFileManager.INSTANCE.addJarFile(artifact);
                                           System.out.println("BOOTSTRAPMODULE original loader " + jar.getName());
                                           ResourceLoader originalLoader = ResourceLoaders.createJarResourceLoader(artifact.getName(), jar);
                                           if (originalLoader == null) {
                                               throw new IOException();
                                           }*/
                                           System.out.println("BOOTSTRAPMODULE loader " + "filtered" + artifact.getAbsolutePath());
                                           return ResourceLoaders.createFilteredResourceLoader(filter, originalLoader);
                                        /*} catch (IOException e) {
                                            return null;
                                        }*/
                                     });

                             if (loader == null) {
                               throw new IOException();
                             }

                            builder.addResourceRoot(
                                    ResourceLoaderSpec.createResourceLoaderSpec(loader
                                    )
                            );

                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });

            builder.addDependency(DependencySpec.createLocalDependencySpec());
            builder.addDependency(DependencySpec.createModuleDependencySpec("org.jboss.modules"));
            builder.addDependency(DependencySpec.createModuleDependencySpec("org.jboss.shrinkwrap"));
            builder.addDependency(DependencySpec.createModuleDependencySpec("org.yaml.snakeyaml"));

            HashSet<String> paths = new HashSet<>();
            paths.add("org/wildfly/swarm/bootstrap/env");
            paths.add("org/wildfly/swarm/bootstrap/logging");
            paths.add("org/wildfly/swarm/bootstrap/modules");
            paths.add("org/wildfly/swarm/bootstrap/performance");
            paths.add("org/wildfly/swarm/bootstrap/util");
            builder.addDependency(DependencySpec.createSystemDependencySpec(paths, true));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private PathFilter getModuleFilter(File file) throws IOException {
        Set<String> paths = new HashSet<>();

        try (JarFile jar = new JarFile(file)) {
            Enumeration<JarEntry> jarEntries = jar.entries();

             while (jarEntries.hasMoreElements()) {
                  JarEntry jarEntry = jarEntries.nextElement();
                 if (!jarEntry.isDirectory()) {
                        String name = jarEntry.getName();
                     if (name.endsWith("/module.xml")) {
                         paths.add(name);
                      }
                 }
            }
        }
        return PathFilters.in(paths);
    }

    private static final BootstrapLogger LOG = BootstrapLogger.logger("org.wildfly.swarm.modules.bootstrap");
}
