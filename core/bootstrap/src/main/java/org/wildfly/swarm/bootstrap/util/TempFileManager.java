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
package org.wildfly.swarm.bootstrap.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Bob McWhirter
 */
public class TempFileManager {

    public static final String TMPDIR_PROPERTY = "swarm.io.tmpdir";

    public static final String WFSWARM_TMP_PREFIX = "wfswarm";

    public static final TempFileManager INSTANCE = new TempFileManager();

    private TempFileManager() {
        registered.put(true, Collections.newSetFromMap(new ConcurrentHashMap<>()));
        registered.put(false, Collections.newSetFromMap(new ConcurrentHashMap<>()));

        String tmpDir = System.getProperty(TMPDIR_PROPERTY);
        if (tmpDir != null) {
            this.tmpDir = new File(tmpDir);
            if (!Files.exists(this.tmpDir.toPath())) {
                try {
                    Files.createDirectories(this.tmpDir.toPath());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        /*Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            INSTANCE.close(true);
        }));*/
    }

    public File newTempDirectory(String base, String ext) throws IOException {
        return newTempDirectory(false, base, ext);
    }

    public File newTempDirectory(boolean shutdownHook, String base, String ext) throws IOException {
        File tmp = File.createTempFile(WFSWARM_TMP_PREFIX + base, ext, this.tmpDir);
        tmp.delete();
        tmp.mkdirs();
        //tmp.deleteOnExit();
        register(shutdownHook, tmp);
        return tmp;
    }

    public File newTempFile(String base, String ext) throws IOException {
        return newTempFile(false, base, ext);
    }

    public File newTempFile(boolean shutdownHook, String base, String ext) throws IOException {
        File tmp = File.createTempFile(WFSWARM_TMP_PREFIX + base, ext, this.tmpDir);
        tmp.delete();
        //tmp.deleteOnExit();
        register(shutdownHook, tmp);
        return tmp;
    }

    private void register(boolean shutdown, File file) {
        this.registered.get(shutdown).add(file);
    }

    public void close() {
        close(false);
    }

    public void close(boolean shutdownHook) {
        Set<File> registeredFiles = registered.get(shutdownHook);
        for (File file : registeredFiles) {
            deleteRecursively(file);
        }
    }

    public static void deleteRecursively(File file) {
        if (!file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    deleteRecursively(child);
                }
            }
        }

        file.delete();
    }

    private Map<Boolean, Set<File>> registered = new ConcurrentHashMap<>();

    private File tmpDir;

}
