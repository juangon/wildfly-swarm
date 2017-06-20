/**
 * Copyright 2016 Red Hat, Inc, and individual contributors.
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
package org.wildfly.swarm.servers.management.console;

import org.wildfly.swarm.Swarm;
import org.wildfly.swarm.management.console.ManagementConsoleFraction;

/**
 * @author George Gastaldi
 */
public class Main {

    private static Swarm container;

    protected Main() {
    }

    public static void main(String... args) throws Exception {
        container = new Swarm();
        container.fraction(new ManagementConsoleFraction());
        container.start();
    }

    public static void stopMain() throws Exception {
        container.stop();
    }
}
