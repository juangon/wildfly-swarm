package org.wildfly.swarm.bootstrap.util;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.jar.JarFile;

public class JarFileManager {

    public static final JarFileManager INSTANCE = new JarFileManager();

    private JarFileManager() {
    }

    public JarFile addJarFile(File file) throws IOException {

        JarFile jarFile = jarFileToClose.get(file);
        if (jarFile == null) {
             jarFile = new JarFile(file);
             jarFileToClose.put(file, jarFile);
             System.out.println("---JAR FILE:" + file.getAbsolutePath() + " added");
        } else {
            System.out.println("---JAR FILE:" + file.getAbsolutePath() + " not added");
        }

        /*if (file.getAbsolutePath().contains("io-2018")) {
              for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
                  System.out.println(ste);
              }
        }*/
        return jarFile;
    }

    public void close() throws IOException {
        jarFileToClose.
                forEach((f, j) -> {
                    System.out.println("---JARFILEMANAGER CLOSING FILE " + f.getAbsolutePath());
                    try {
                        j.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    });
    }

    private Map<File, JarFile> jarFileToClose = new LinkedHashMap<>();
}
