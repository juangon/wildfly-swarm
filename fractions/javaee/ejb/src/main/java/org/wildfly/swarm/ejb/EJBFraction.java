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
package org.wildfly.swarm.ejb;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.wildfly.swarm.config.EJB3;
import org.wildfly.swarm.config.ejb3.ChannelCreationOptions;
import org.wildfly.swarm.config.ejb3.StrictMaxBeanInstancePool;
import org.wildfly.swarm.spi.api.Fraction;
import org.wildfly.swarm.spi.api.SwarmProperties;
import org.wildfly.swarm.spi.api.annotations.Configurable;
import org.wildfly.swarm.spi.api.annotations.MarshalDMR;
import org.wildfly.swarm.spi.api.annotations.WildFlyExtension;

/**
 * @author Ken Finnigan
 * @author Lance Ball
 */
@WildFlyExtension(module = "org.jboss.as.ejb3")
@MarshalDMR
@Configurable("swarm.ejb3")
public class EJBFraction extends EJB3<EJBFraction> implements Fraction<EJBFraction> {

    private static final String DEFAULT = "default";

    @PostConstruct
    public void postConstruct() {
        applyDefaults();
    }

    public static EJBFraction createDefaultFraction() {
        return new EJBFraction().applyDefaults();
    }

    public EJBFraction applyDefaults() {
        Map<Object, Object> threadPoolSettings = new HashMap<>();
        threadPoolSettings.put("time", "100");
        threadPoolSettings.put("unit", "MILLISECONDS");

        this
                .defaultSlsbInstancePool("slsb-strict-max-pool")
                .defaultStatefulBeanAccessTimeout(5000L)
                .defaultSfsbCache("simple")
                .defaultSfsbPassivationDisabledCache("simple")
                .defaultSingletonBeanAccessTimeout(5000L)
                .defaultResourceAdapterName(SwarmProperties.propertyVar("ejb.resource-adapter-name", "activemq-ra.rar"))
                .defaultMdbInstancePool("mdb-strict-max-pool")
                .strictMaxBeanInstancePool("slsb-strict-max-pool", pool -> {
                    pool.deriveSize(StrictMaxBeanInstancePool.DeriveSize.FROM_WORKER_POOLS);
                    pool.timeout(5L);
                    pool.timeoutUnit(StrictMaxBeanInstancePool.TimeoutUnit.MINUTES);
                })
                .strictMaxBeanInstancePool("mdb-strict-max-pool", pool -> {
                        pool.deriveSize(StrictMaxBeanInstancePool.DeriveSize.FROM_CPU_COUNT);
                        pool.timeout(5L);
                        pool.timeoutUnit(StrictMaxBeanInstancePool.TimeoutUnit.MINUTES);
                })
                .cache("simple")
                .cache("distributable", cache -> {
                    cache.alias("passivating");
                    cache.alias("clustered");
                    cache.passivationStore("infinispan");
                })
                .passivationStore("infinispan", store -> {
                    store.cacheContainer("ejb");
                    store.maxSize(10000);
                })
                .asyncService(async -> {
                    async.threadPoolName(DEFAULT);
                })
                .timerService(timer -> {
                    timer.threadPoolName(DEFAULT);
                    timer.defaultDataStore("default-file-store");
                    timer.fileDataStore("default-file-store", store -> {
                        store.path("timer-service-data");
                        store.relativeTo("jboss.server.data.dir"); // TODO
                    });
                })
                .remoteService(remote -> {
                    remote.connectorRef("http-remoting-connector");
                    remote.threadPoolName(DEFAULT);
                    remote.channelCreationOptions("READ_TIMEOUT", opt -> {
                        opt.value(SwarmProperties.propertyVar("prop.remoting-connector.read.timeout", "20"));
                        opt.type(ChannelCreationOptions.Type.XNIO);
                    });
                    remote.channelCreationOptions("MAX_OUTBOUND_MESSAGES", opt -> {
                        opt.value("1234");
                        opt.type(ChannelCreationOptions.Type.REMOTING);
                    });
                })
                .threadPool(DEFAULT, threadPool -> {
                    threadPool.maxThreads(10);
                    threadPool.keepaliveTime(threadPoolSettings);
                })
                .iiopService(iiop -> {
                    iiop.enableByDefault(false);
                    iiop.useQualifiedName(false);
                })
                .defaultSecurityDomain("other")
                .defaultMissingMethodPermissionsDenyAccess(true)
                .logSystemExceptions(true);

        return this;
    }

}
