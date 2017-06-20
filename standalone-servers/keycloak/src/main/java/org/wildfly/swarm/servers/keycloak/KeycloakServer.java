package org.wildfly.swarm.servers.keycloak;

import org.wildfly.swarm.Swarm;

/**
 * @author Ken Finnigan
 */
public class KeycloakServer {

    private static Swarm swarm;

    protected KeycloakServer() {
    }

    public static void main(String... args) throws Exception {
        swarm = (new Swarm(args)).start();
    }

    public static void stopMain() throws Exception {
        swarm.stop();
    }
}
