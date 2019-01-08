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
package org.wildfly.swarm.arquillian.adapter;

import org.jboss.arquillian.container.spi.ConfigurationException;
import org.wildfly.swarm.arquillian.daemon.container.DaemonContainerConfigurationBase;

/**
 * @author Bob McWhirter
 */
public class WildFlySwarmContainerConfiguration extends DaemonContainerConfigurationBase {

    @Override
    public void validate() throws ConfigurationException {
        super.validate();

        System.out.print("-------FRACTIONDETECTIONMODE:" + this.fractionDetectionMode);
        if (fractionDetectionMode == null || fractionDetectionMode.length() == 0
                || (!fractionDetectionMode.equals("force") && !fractionDetectionMode.equals("never"))) {
            this.fractionDetectionMode = "when_missing";
        }
    }

    public String getFractionDetectionMode() {
        return fractionDetectionMode;
    }

    public void setFractionDetectionMode(String fractionDetectionMode) {
        this.fractionDetectionMode = fractionDetectionMode;
    }

    private String fractionDetectionMode;

}
