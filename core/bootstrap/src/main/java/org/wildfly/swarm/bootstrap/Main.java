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
package org.wildfly.swarm.bootstrap;

import static java.nio.file.StandardWatchEventKinds.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.WatchEvent.Kind;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.wildfly.swarm.bootstrap.env.ApplicationEnvironment;
import org.wildfly.swarm.bootstrap.modules.BootModuleLoader;
import org.wildfly.swarm.bootstrap.performance.Performance;
import org.wildfly.swarm.bootstrap.util.BootstrapProperties;

/**
 * @author Bob McWhirter
 */
public class Main {

    public Main(String... args) throws Throwable {
        this.args = args;
    }

    public static void main(String... args) throws Throwable {
        try {
            System.out.println("------BOOOSTRAP!");
            Performance.start();
            //TODO Move property key to -spi
            System.setProperty(BootstrapProperties.IS_UBERJAR, Boolean.TRUE.toString());

            String processFile = System.getProperty("org.wildfly.swarm.mainProcessFile");

            System.out.println("Main process file " + processFile);
            if (processFile != null) {
                shutdownService = Executors.newSingleThreadExecutor();
                shutdownService.submit(() -> {
                    File uuidFile = new File(processFile);
                    try {
                        register(uuidFile.getParentFile(), uuidFile.toPath());
                        processEvents(uuidFile.toPath());
                        //Swarm.this.stop();
                        if (mainInvoker != null) {
                            mainInvoker.stop();
                        }
                        //shutdownService.shutdownNow();
                        System.out.println("--------GONNNA EXIT");
                        System.exit(0);
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    return null;
                    //Swarm.this.stop();return null;
                    }
                );
            }

            new Main(args).run();
        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        }
    }

    private static void register(File directory, Path file) throws IOException {
        System.out.println("MAIN REGISTERING DIRECTORY " + directory);
        watcher = FileSystems.getDefault().newWatchService();
        keys = new HashMap<WatchKey,Path>();
        WatchKey key = directory.toPath().register(watcher, ENTRY_DELETE, ENTRY_CREATE);
        keys.put(key, directory.toPath());
        System.out.println("MAIN FILE REGISTERED");
    }

    private static void processEvents(Path file) {
        for (;;) {

            System.out.println("MAIN PROCESSING EVENTS");
            // wait for key to be signalled
            WatchKey key;
            try {
                key = watcher.take();
                System.out.println("MAIN SIGNAL FILE");
            } catch (InterruptedException x) {
                return;
            }

            Path dir = keys.get(key);
            if (dir == null) {
                System.err.println("WatchKey not recognized!!");
                continue;
            }

            for (WatchEvent<?> event: key.pollEvents()) {
                Kind<?> kind = event.kind();

                // Context for directory entry event is the file name of entry
                WatchEvent<Path> ev = (WatchEvent<Path>)event;
                Path name = ev.context();
                Path child = dir.resolve(name);

                // print out event
                System.out.format("%s: %s\n", event.kind().name(), child);

                if (kind == ENTRY_DELETE && child.equals(file)) {
                    System.out.println("MAIN FILE DELETED");
                    return;
                }
            }

            // reset key and remove from set if directory no longer accessible
            boolean valid = key.reset();
            if (!valid) {
                keys.remove(key);

                // all directories are inaccessible
                if (keys.isEmpty()) {
                    break;
                }
            }
        }
    }

    public void run() throws Throwable {
        setupBootModuleLoader();
        mainInvoker = new MainInvoker(ApplicationEnvironment.get().getMainClassName(), this.args);
        mainInvoker.invoke();
    }

    public void setupBootModuleLoader() {
        System.setProperty("boot.module.loader", BootModuleLoader.class.getName());
    }

    private final String[] args;

    private static WatchService watcher;

    private static Map<WatchKey,Path> keys;

    private static ExecutorService shutdownService;

    private static MainInvoker mainInvoker;
}
