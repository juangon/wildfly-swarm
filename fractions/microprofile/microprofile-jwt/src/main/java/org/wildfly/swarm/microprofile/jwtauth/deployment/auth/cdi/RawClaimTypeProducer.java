/**
 *
 *   Copyright 2017 Red Hat, Inc, and individual contributors.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.wildfly.swarm.microprofile.jwtauth.deployment.auth.cdi;

import java.lang.annotation.Annotation;
import java.util.Optional;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Named;

import org.eclipse.microprofile.jwt.Claim;
import org.eclipse.microprofile.jwt.ClaimValue;
import org.eclipse.microprofile.jwt.Claims;
import org.jboss.logging.Logger;

/**
 *
 */
public class RawClaimTypeProducer {
    private static Logger log = Logger.getLogger(RawClaimTypeProducer.class);
    @Produces
    @Claim("")
    @Named("RawClaimTypeProducer#getValue")
    public Object getValue(InjectionPoint ip) {
        log.debugf("getValue(%s)", ip);
        String name = getName(ip);
        ClaimValue<Optional<Object>> cv = MPJWTProducer.generalClaimValueProducer(name);
        Optional<Object> value = cv.getValue();
        Object returnValue = value.orElse(null);
        return returnValue;
    }

    @Produces
    @Claim("")
    @Named("RawClaimTypeProducer#getOptionalValue")
    public Optional getOptionalValue(InjectionPoint ip) {
        log.debugf("getOptionalValue(%s)", ip);
        String name = getName(ip);
        ClaimValue<Optional<Object>> cv = MPJWTProducer.generalClaimValueProducer(name);
        Optional<Object> value = cv.getValue();
        return value;
    }

    String getName(InjectionPoint ip) {
        String name = null;
        for (Annotation ann : ip.getQualifiers()) {
            if (ann instanceof Claim) {
                Claim claim = (Claim) ann;
                name = claim.standard() == Claims.UNKNOWN ? claim.value() : claim.standard().name();
            }
        }
        return name;
    }
}
