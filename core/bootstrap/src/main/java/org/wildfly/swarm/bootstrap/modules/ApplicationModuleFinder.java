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
package org.wildfly.swarm.bootstrap.modules;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
//import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.jboss.modules.DependencySpec;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.ModuleSpec;
import org.jboss.modules.ResourceLoader;
import org.jboss.modules.ResourceLoaderSpec;
import org.jboss.modules.ResourceLoaders;
import org.jboss.modules.filter.ClassFilters;
import org.jboss.modules.filter.PathFilter;
import org.jboss.modules.filter.PathFilters;
import org.jboss.modules.maven.ArtifactCoordinates;
import org.wildfly.swarm.bootstrap.env.ApplicationEnvironment;
import org.wildfly.swarm.bootstrap.logging.BootstrapLogger;
import org.wildfly.swarm.bootstrap.util.TempFileManager;
//import org.wildfly.swarm.bootstrap.util.TempFileManager;
//import org.wildfly.swarm.bootstrap.util.TempFileManager;

/**
 * Module-finder used only for loading the module <code>swarm.application</code> when run in an fat-jar scenario.
 *
 * @author Bob McWhirter
 */
public class ApplicationModuleFinder extends AbstractSingleModuleFinder {

    public static final String MODULE_NAME = "swarm.application";

    public ApplicationModuleFinder() {
        super(MODULE_NAME);
    }

    protected ApplicationModuleFinder(String slot) {
        super(MODULE_NAME, slot);
    }

    @Override
    public void buildModule(ModuleSpec.Builder builder, ModuleLoader delegateLoader) throws ModuleLoadException {

        ApplicationEnvironment env = ApplicationEnvironment.get();


        env.bootstrapModules()
                .forEach((module) -> {
                    builder.addDependency(
                            DependencySpec.createModuleDependencySpec(
                                    PathFilters.acceptAll(),
                                    PathFilters.acceptAll(),
                                    PathFilters.acceptAll(),
                                    PathFilters.acceptAll(),
                                    ClassFilters.acceptAll(),
                                    ClassFilters.acceptAll(),
                                    null,
                                    ModuleIdentifier.create(module), false));

                });

        try {
            addAsset(builder, env);
        } catch (IOException e) {
            throw new ModuleLoadException(e);
        }

        addDependencies(builder, env);

        builder.addDependency(DependencySpec.createModuleDependencySpec(ModuleIdentifier.create("org.jboss.modules")));
        builder.addDependency(DependencySpec.createModuleDependencySpec(ModuleIdentifier.create("org.jboss.msc")));
        builder.addDependency(DependencySpec.createModuleDependencySpec(ModuleIdentifier.create("org.jboss.shrinkwrap")));
        builder.addDependency(DependencySpec.createModuleDependencySpec(ModuleIdentifier.create("org.wildfly.swarm.configuration"), false, true));
        builder.addDependency(DependencySpec.createModuleDependencySpec(ModuleIdentifier.create("javax.api")));
        builder.addDependency(DependencySpec.createModuleDependencySpec(ModuleIdentifier.create("sun.jdk"), false, true));

        builder.addDependency(
                DependencySpec.createModuleDependencySpec(
                        PathFilters.acceptAll(),
                        PathFilters.acceptAll(),
                        PathFilters.acceptAll(),
                        PathFilters.acceptAll(),
                        ClassFilters.acceptAll(),
                        ClassFilters.acceptAll(),
                        null,
                        ModuleIdentifier.create("org.wildfly.swarm.container", "api"), true));

        builder.addDependency(DependencySpec.createLocalDependencySpec());
    }

    protected void addAsset(ModuleSpec.Builder builder, ApplicationEnvironment env) throws IOException {
        String path = env.getAsset();
        if (path == null) {
            return;
        }

        int slashLoc = path.lastIndexOf('/');
        String name = path;

        if (slashLoc > 0) {
            name = path.substring(slashLoc + 1);
        }

        String ext = ".jar";
        int dotLoc = name.lastIndexOf('.');
        if (dotLoc > 0) {
            ext = name.substring(dotLoc);
            name = name.substring(0, dotLoc);
        }

        //File tmp = TempFileManager.INSTANCE.newTempFile(name, ext);
        File tmp = File.createTempFile(name, ext);
        //File tmp = TempFileManager.INSTANCE.newTempDirectory(name, ext);
        //File tmp = File.createTempFile(name, ext);
        //tmp.deleteOnExit();
        //OutputStream out = new FileOutputStream(tmp);

        try (InputStream artifactIn = getClass().getClassLoader().getResourceAsStream(path)) {
            //IOUtil.copy(artifactIn, new FileOutputStream(tmp));
            copyFile(artifactIn, tmp);
            //Files.copy(artifactIn, tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        final String jarName = tmp.getName().toString();
        final JarFile jarFile = new JarFile(tmp);

        if (".war".equalsIgnoreCase(ext)) {

            File tmpDir = TempFileManager.INSTANCE.newTempDirectory(name, ext);
            explodeJar(jarFile, tmpDir.getAbsolutePath(), null);

            jarFile.close();
            tmp.delete();

            /*final ResourceLoader warLoader = ResourceLoaders.createJarResourceLoader(
                    jarName, jarFile, "WEB-INF/classes");
            builder.addResourceRoot(ResourceLoaderSpec.createResourceLoaderSpec(warLoader));

            final ResourceLoader jarLoader = ResourceLoaders.createJarResourceLoader("",
                    jarFile);
            builder.addResourceRoot(ResourceLoaderSpec.createResourceLoaderSpec(jarLoader));*/

            /*
             final ResourceLoader warLoader = ResourceLoaders.createJarResourceLoader("",
                                                                                     jarFile,
                                                                                    "WEB-INF/classes");
              builder.addResourceRoot(ResourceLoaderSpec.createResourceLoaderSpec(warLoader));

              final ResourceLoader jarLoader = ResourceLoaders.createJarResourceLoader("2",
                    jarFile2, "");
            builder.addResourceRoot(ResourceLoaderSpec.createResourceLoaderSpec(jarLoader));*/

            /*final ResourceLoader jarLoader = ResourceLoaders.createJarResourceLoader("2",
                    jarFile);
            builder.addResourceRoot(ResourceLoaderSpec.createResourceLoaderSpec(jarLoader));

            final ResourceLoader warLoader = ResourceLoaders.createJarResourceLoader("",
                                                                                     jarFile,
                                                                                    "WEB-INF/classes");
            builder.addResourceRoot(ResourceLoaderSpec.createResourceLoaderSpec(warLoader));*/

            /*final ResourceLoader originalResourceLoader = ResourceLoaders.createJarResourceLoader(jarFile.getName(),
                    jarFile);

            final ResourceLoader filteredResourceLoader = ResourceLoaders.createFilteredResourceLoader(getModuleFilter(jarFile), originalResourceLoader);

            builder.addResourceRoot(ResourceLoaderSpec.createResourceLoaderSpec(filteredResourceLoader));*/

            System.out.println("WAR LOADER ROOT:" + tmpDir.getAbsolutePath() + File.separator + "WEB-INF" + File.separator + "classes");
            final ResourceLoader warLoader = ResourceLoaders.createFileResourceLoader(jarName,
                    new File(tmpDir.getAbsolutePath() + File.separator + "WEB-INF" + File.separator + "classes"));
            builder.addResourceRoot(ResourceLoaderSpec.createResourceLoaderSpec(warLoader));

            final ResourceLoader jarLoader = ResourceLoaders.createFileResourceLoader("", tmpDir);
            builder.addResourceRoot(ResourceLoaderSpec.createResourceLoaderSpec(jarLoader));

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
               //try {
                    System.out.println(".--------YEAAAAAH2");
                    //jarLoader.close();
                    //originalResourceLoader.close();
                    //jarFile.close();
                    //jarFile2.close();
                    /*while (tmp.exists()) {
                        tmp.delete();
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }*/
                    System.out.println(".--------YEAAAAAH2 BORRADO2 FICHERO 1 "/* + tmp.delete()*/);
                    //System.out.println(".--------YEAAAAAH2 BORRADO2 FICHERO 2 " + tmp2.delete());
                    //warLoader.close();
                    /*jarFile2.close();
                    tmp2.delete();*/
                    System.out.println(".--------YEAAAAAH2 TERMINADO3 ");
                //} catch (IOException e) {
                    // TODO Auto-generated catch block
                //    e.printStackTrace();
                //}
                 //System.out.println(".--------YEAAAAAH2 BORRANDO FICHERO");
                //while (tmp.exists()) {
                //}
                System.out.println(".--------YEAAAAAH2 FICHERO BORRADO");
            }));

        } else {

            File tmpDir = TempFileManager.INSTANCE.newTempDirectory(name, ext);
            explodeJar(jarFile, tmpDir.getAbsolutePath(), null);

            jarFile.close();
            tmp.delete();

            final ResourceLoader jarLoader = ResourceLoaders.createFileResourceLoader("", tmpDir);
            builder.addResourceRoot(ResourceLoaderSpec.createResourceLoaderSpec(jarLoader));

            /*final ResourceLoader jarLoader = ResourceLoaders.createJarResourceLoader(jarName,
                    jarFile);
            builder.addResourceRoot(ResourceLoaderSpec.createResourceLoaderSpec(jarLoader));*/

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                //try {
                    System.out.println(".--------YEAAAAAH");
                    //jarLoader.close();
                    //jarFile.close();
                //} catch (IOException e) {
                    // TODO Auto-generated catch block
                //   e.printStackTrace();
                //}
                //tmp.delete();
            }));
        }

    }

    protected void addDependencies(ModuleSpec.Builder builder, ApplicationEnvironment env) {
        env.getDependencies()
                .forEach((dep) -> {
                    String[] parts = dep.split(":");
                    ArtifactCoordinates coords = null;

                    if (!parts[2].equals("jar")) {
                        return;
                    }

                    if (parts.length == 4) {
                        coords = new ArtifactCoordinates(parts[0], parts[1], parts[3]);
                    } else if (parts.length == 5) {
                        coords = new ArtifactCoordinates(parts[0], parts[1], parts[4], parts[3]);
                    }
                    try {
                        File artifact = MavenResolvers.get().resolveJarArtifact(coords);
                        if (artifact == null) {
                            LOG.error("Unable to find artifact for " + coords);
                            return;
                        }
                        JarFile jar = new JarFile(artifact);

                        builder.addResourceRoot(
                                ResourceLoaderSpec.createResourceLoaderSpec(
                                        ResourceLoaders.createJarResourceLoader(artifact.getName(), jar)
                                )
                        );
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    protected static void copyFile(final InputStream in, final File dest) throws IOException {
        dest.getParentFile().mkdirs();
        byte[] buff = new byte[1024];
        final OutputStream out = new BufferedOutputStream(new FileOutputStream(dest));
        try {
            int i = in.read(buff);
            while (i > 0) {
                out.write(buff, 0, i);
                i = in.read(buff);
            }
        } finally {
            close(out);
        }
    }

    protected static void close(Closeable closeable) {
      try {
          closeable.close();
       } catch (IOException ignore) {
       }
    }

    private void explodeJar(JarFile jarFile, String destdir, String rootDir) throws IOException {
        Enumeration<java.util.jar.JarEntry> enu = jarFile.entries();
        while (enu.hasMoreElements()) {
            JarEntry je = enu.nextElement();

            //System.out.println(je.getName());

            File fl = new File(destdir, je.getName());
            if (!fl.exists()) {
                fl.getParentFile().mkdirs();
                fl = new File(destdir, je.getName());
            }
            if (je.isDirectory()) {
                continue;
            }
            InputStream is = null;
            FileOutputStream fo = null;
            try {
                is = jarFile.getInputStream(je);
                fo = new FileOutputStream(fl);
                while (is.available() > 0) {
                    fo.write(is.read());
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (fo != null) {
                    fo.close();
                }
                if (is != null) {
                    is.close();
                }
            }
        }
    }

    private PathFilter getModuleFilter(JarFile jar) {
        Set<String> paths = new HashSet<>();

        Enumeration<JarEntry> jarEntries = jar.entries();

        while (jarEntries.hasMoreElements()) {
            JarEntry jarEntry = jarEntries.nextElement();
            if (!jarEntry.isDirectory()) {
                String name = jarEntry.getName();
                System.out.println("Name:" + name);
                if (name.startsWith("WEB-INF/classes") || name.startsWith("META-INF") ||
                        !name.contains("/")) {
                    paths.add(name);
                }
            }
        }
        System.out.println("Paths added:" + PathFilters.in(paths).toString());
        return PathFilters.in(paths);
    }

    private static final BootstrapLogger LOG = BootstrapLogger.logger("org.wildfly.swarm.modules.application");
}
