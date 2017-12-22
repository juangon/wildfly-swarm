package org.wildfly.swarm.bootstrap.util;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.jboss.modules.ResourceLoader;

public class ResourceLoaderManager {

    public static final ResourceLoaderManager INSTANCE = new ResourceLoaderManager();

    private ResourceLoaderManager() {
    }

    public ResourceLoader addResourceLoader(Path path, Supplier<ResourceLoader> resourceLoaderSupplier) {
        return addResourceLoader(path.toString(), resourceLoaderSupplier);
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
                    System.out.println("---RESOURCELOADERMANAGER CLOSING RESOURCELOADER " + r.getLocation());
                    r.close();
                    });
    }

    private Map<String, ResourceLoader> resourceLoaderToClose = new HashMap<>();
}
