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

import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;

import java.io.Closeable;
import java.io.File;
//import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
//import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.WatchEvent.Kind;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.modules.maven.ArtifactCoordinates;

import org.jboss.modules.maven.MavenResolver;
//import org.wildfly.swarm.bootstrap.util.TempFileManager;

/**
 * @author Bob McWhirter
 */
public class UberJarMavenResolver implements MavenResolver, Closeable {

    private static final String HYPHEN = "-";

    private static final String DOT = ".";

    private Map<ArtifactCoordinates, File> resolutionCache = new ConcurrentHashMap<>();

    //private static ExecutorService monitorService;

    private static WatchService watcher;

    public static File copyTempJar(String artifactId, InputStream in, String packaging) throws IOException {
       //File tmp = TempFileManager.INSTANCE.newTempFile(artifactId, DOT + packaging);
       File tmp = Files.createTempFile(artifactId, DOT + packaging).toFile();
       /*byte[] buffer = new byte[in.available()];
       in.read(buffer);

       OutputStream outStream = new FileOutputStream(tmp);
       outStream.write(buffer);
       outStream.close();*/
       Files.copy(in, tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
       in.close();

       /*if (tmp.getName().contains("asm")) {
           RandomAccessFile rf = new RandomAccessFile(tmp, "rw");
           FileChannel fileChannel = rf.getChannel();

           try {
               FileLock lock = fileChannel.lock();
               lock.acquiredBy().toString();
           } catch (Exception e) {
               e.printStackTrace();
           }

//           monitorService = Executors.newSingleThreadExecutor();
//           monitorService.submit(() -> {
//               File uuidFile = tmp;
//                   try {
//                       File watchedDir = uuidFile.getParentFile();
//                       register(watchedDir);
//                       processEvents(watchedDir, uuidFile.toPath());
//                   } catch (Exception e) {
//                       e.printStackTrace();
//                   }
//
//                   return null;
//               }
//           );
       }*/
       System.out.println("Copying " + tmp.toString());
       return tmp;
    }

    private static void register(File directory) throws IOException {
        watcher = FileSystems.getDefault().newWatchService();
        directory.toPath().register(watcher, ENTRY_DELETE);
    }

    @SuppressWarnings("unchecked")
    private static void processEvents(File watchedDir, Path file) {
        for (;;) {

            WatchKey key;
            try {
                key = watcher.take();
            } catch (InterruptedException x) {
                return;
            }

            for (WatchEvent<?> event: key.pollEvents()) {
                Kind<?> kind = event.kind();

                WatchEvent<Path> ev = (WatchEvent<Path>)event;
                Path name = ev.context();
                Path child = watchedDir.toPath().resolve(name);

                if (child.equals(file)) {
                    System.out.println("---FILE:" + file + " event:" + kind);
                    if (kind == ENTRY_DELETE) {
                        return;
                    }
                }
            }

            boolean valid = key.reset();
            if (!valid) {
               break;
            }
        }
    }

    @Override
    public File resolveArtifact(ArtifactCoordinates coordinates, String packaging) throws IOException {

        File resolved = this.resolutionCache.get(coordinates);
        if (resolved == null) {

            String artifactRelativePath = "m2repo/" + relativeArtifactPath('/', coordinates.getGroupId(), coordinates.getArtifactId(), coordinates.getVersion());
            String classifier = "";
            if (coordinates.getClassifier() != null && !coordinates.getClassifier().trim().isEmpty()) {
                classifier = HYPHEN + coordinates.getClassifier();
            }

            String jarPath = artifactRelativePath + classifier + DOT + packaging;

            InputStream stream = UberJarMavenResolver.class.getClassLoader().getResourceAsStream(jarPath);

            if (stream != null) {
                resolved = copyTempJar(coordinates.getArtifactId() + HYPHEN + coordinates.getVersion(), stream, packaging);
                this.resolutionCache.put(coordinates, resolved);
            }
        }

        return resolved;
    }

    @Override
    public void close() throws IOException {
        resolutionCache.forEach((a, f) -> {
                System.out.println(" Deleting jar " + f.getName());
                try {
                    Files.delete(f.toPath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
        });
    }

    static String relativeArtifactPath(char separator, String groupId, String artifactId, String version) {
        StringBuilder builder = new StringBuilder(groupId.replace('.', separator));
        builder.append(separator).append(artifactId).append(separator);
        String pathVersion;
        final Matcher versionMatcher = snapshotPattern.matcher(version);
        if (versionMatcher.find()) {
            // it's really a snapshot
            pathVersion = version.substring(0, versionMatcher.start()) + "-SNAPSHOT";
        } else {
            pathVersion = version;
        }
        builder.append(pathVersion).append(separator).append(artifactId).append(HYPHEN).append(version);
        return builder.toString();
    }

    private static final Pattern snapshotPattern = Pattern.compile("-\\d{8}\\.\\d{6}-\\d+$");

}
