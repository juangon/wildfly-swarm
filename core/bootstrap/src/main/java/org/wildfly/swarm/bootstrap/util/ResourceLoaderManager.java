package org.wildfly.swarm.bootstrap.util;

import java.io.File;
import java.io.IOException;
//import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.jboss.modules.ResourceLoader;

public class ResourceLoaderManager {

    public static final ResourceLoaderManager INSTANCE = new ResourceLoaderManager();

    private ResourceLoaderManager() {
    }

    public ResourceLoader addResourceLoader(File file, Supplier<ResourceLoader> resourceLoaderSupplier) {
        return addResourceLoader(file.getAbsolutePath(), resourceLoaderSupplier);
    }

    public ResourceLoader addResourceLoader(String name, Supplier<ResourceLoader> resourceLoaderSupplier) {

        ResourceLoader resourceLoader = resourceLoaderToClose.get(name);
        if (resourceLoader == null) {
            resourceLoader = resourceLoaderSupplier.get();
            resourceLoaderToClose.put(name, resourceLoader);
        }

        return resourceLoader;
    }

    public void close() throws IOException {
        resourceLoaderToClose.
                forEach((p, r) -> {
                    r.close();
                }
         );
    }

    private Map<String, ResourceLoader> resourceLoaderToClose = new LinkedHashMap<>();
}
