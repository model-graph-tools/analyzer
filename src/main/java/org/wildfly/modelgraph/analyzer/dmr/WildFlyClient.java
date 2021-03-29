package org.wildfly.modelgraph.analyzer.dmr;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.RealmCallback;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.modelgraph.analyzer.HostAndPort;

import static org.wildfly.modelgraph.analyzer.dmr.ModelDescriptionConstants.*;

public class WildFlyClient implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(WildFlyClient.class);

    private final HostAndPort hostAndPort;
    private final ModelControllerClient mcc;

    public WildFlyClient(HostAndPort hostAndPort, String username, String password) {
        this.hostAndPort = hostAndPort;
        try {
            mcc = ModelControllerClient.Factory.create(InetAddress.getByName(hostAndPort.host()),
                    hostAndPort.port(),
                    callbacks -> {
                        for (var current : callbacks) {
                            if (current instanceof NameCallback) {
                                var ncb = (NameCallback) current;
                                ncb.setName(username);
                            } else if (current instanceof PasswordCallback) {
                                var pcb = (PasswordCallback) current;
                                pcb.setPassword(password.toCharArray());
                            } else if (current instanceof RealmCallback) {
                                var rcb = (RealmCallback) current;
                                rcb.setText(rcb.getDefaultText());
                            } else {
                                throw new UnsupportedCallbackException(current);
                            }
                        }
                    });
            logger.info("Connected to WildFly instance at {}", hostAndPort);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public ModelNode execute(Operation operation) {
        var result = new ModelNode();
        try {
            logger.debug("Execute operation {}", operation);
            var modelNode = mcc.execute(operation);
            if (modelNode.hasDefined(OUTCOME)) {
                var outcome = modelNode.get(OUTCOME).asString();
                if (SUCCESS.equals(outcome)) {
                    if (modelNode.hasDefined(RESULT)) {
                        result = modelNode.get(RESULT);
                    }
                } else if (FAILED.equals(outcome)) {
                    if (modelNode.hasDefined(FAILURE_DESCRIPTION)) {
                        var error = modelNode.get(FAILURE_DESCRIPTION).asString();
                        logger.error("Unable to execute {}: {}", operation.asCli(), error);
                    }
                } else {
                    logger.error("Unable to execute {}: Unknown outcome {}", operation.asCli(), outcome);
                }
            } else {
                logger.error("Unable to execute {}: No outcome", operation.asCli());
            }
        } catch (IOException e) {
            logger.error("Unable to execute {}: {}", operation.asCli(), e.getMessage());
        }
        return result;
    }

    @Override
    public void close() {
        logger.debug("Closing connection to WildFly instance");
        try {
            mcc.close();
        } catch (IOException e) {
            logger.error("Unable to close connection to WildFly instance at {}: {}", hostAndPort, e.getMessage());
        }
    }
}
