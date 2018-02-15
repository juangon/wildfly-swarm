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

import java.io.IOException;

import org.jboss.modules.ModuleFinder;
import org.jboss.modules.ModuleLoader;

/**
 * @author Bob McWhirter
 */
public class BootModuleLoader extends ModuleLoader implements AutoCloseable {

    public BootModuleLoader() throws IOException {
        super(new ModuleFinder[]{
                new BootstrapClasspathModuleFinder(),
                new BootstrapModuleFinder(),
                new ClasspathModuleFinder(),
                new ContainerModuleFinder(),
                new ApplicationModuleFinder(),
                new DynamicModuleFinder(),
        });
    }

    public void close() {
        final ModuleFinder[] finders = getFinders();
        for (ModuleFinder finder : finders) {
            if (finder instanceof AutoCloseable) {
                try {
                    ((AutoCloseable)finder).close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
