package org.wildfly.modelgraph.analyzer.dmr;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.RealmCallback;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.modelgraph.analyzer.HostAndPort;
import org.wildfly.modelgraph.analyzer.Version;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import static org.wildfly.modelgraph.analyzer.dmr.ModelDescriptionConstants.ATTRIBUTES_ONLY;
import static org.wildfly.modelgraph.analyzer.dmr.ModelDescriptionConstants.FAILED;
import static org.wildfly.modelgraph.analyzer.dmr.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.wildfly.modelgraph.analyzer.dmr.ModelDescriptionConstants.INCLUDE_ALIASES;
import static org.wildfly.modelgraph.analyzer.dmr.ModelDescriptionConstants.INCLUDE_RUNTIME;
import static org.wildfly.modelgraph.analyzer.dmr.ModelDescriptionConstants.INCLUDE_SINGLETONS;
import static org.wildfly.modelgraph.analyzer.dmr.ModelDescriptionConstants.MANAGEMENT_MAJOR_VERSION;
import static org.wildfly.modelgraph.analyzer.dmr.ModelDescriptionConstants.MANAGEMENT_MICRO_VERSION;
import static org.wildfly.modelgraph.analyzer.dmr.ModelDescriptionConstants.MANAGEMENT_MINOR_VERSION;
import static org.wildfly.modelgraph.analyzer.dmr.ModelDescriptionConstants.OPERATIONS;
import static org.wildfly.modelgraph.analyzer.dmr.ModelDescriptionConstants.OUTCOME;
import static org.wildfly.modelgraph.analyzer.dmr.ModelDescriptionConstants.PRODUCT_VERSION;
import static org.wildfly.modelgraph.analyzer.dmr.ModelDescriptionConstants.READ_CHILDREN_TYPES;
import static org.wildfly.modelgraph.analyzer.dmr.ModelDescriptionConstants.READ_RESOURCE;
import static org.wildfly.modelgraph.analyzer.dmr.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION;
import static org.wildfly.modelgraph.analyzer.dmr.ModelDescriptionConstants.RESULT;
import static org.wildfly.modelgraph.analyzer.dmr.ModelDescriptionConstants.SUCCESS;

public class WildFlyInstance implements ManagementModel {

    private static final Logger logger = LoggerFactory.getLogger(WildFlyInstance.class);

    private final HostAndPort hostAndPort;
    private final ModelControllerClient mcc;

    public WildFlyInstance(HostAndPort hostAndPort, String username, String password) {
        this.hostAndPort = hostAndPort;
        try {
            mcc = ModelControllerClient.Factory.create(InetAddress.getByName(hostAndPort.host()), hostAndPort.port(),
                    callbacks -> {
                        for (var current : callbacks) {
                            switch (current) {
                                case NameCallback ncb -> ncb.setName(username);
                                case PasswordCallback pcb -> pcb.setPassword(password.toCharArray());
                                case RealmCallback rcb -> rcb.setText(rcb.getDefaultText());
                                case null, default -> throw new UnsupportedCallbackException(current);
                            }
                        }
                    });
            logger.info("Connected to WildFly instance at {}", hostAndPort);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Identity identity() {
        var operation = new Operation.Builder(READ_RESOURCE, ResourceAddress.of("/"))
                .param(ATTRIBUTES_ONLY, true)
                .param(INCLUDE_RUNTIME, true)
                .build();
        ModelNode node = execute(operation);
        String version = node.get(PRODUCT_VERSION).asString("0.0.0");
        int major = node.get(MANAGEMENT_MAJOR_VERSION).asInt();
        int minor = node.get(MANAGEMENT_MINOR_VERSION).asInt();
        int patch = node.get(MANAGEMENT_MICRO_VERSION).asInt();
        return Identity.wildFly(Version.parse(version), new Version(major, minor, patch));
    }

    @Override
    public List<String> children(ResourceAddress address) {
        var rct = new Operation.Builder(READ_CHILDREN_TYPES, address)
                .param(INCLUDE_SINGLETONS, true)
                .build();

        var result = execute(rct);
        if (result.isDefined()) {
            return result.asList().stream().map(ModelNode::asString).collect(toList());
        }
        return emptyList();
    }

    @Override
    public ModelNode resourceDescription(ResourceAddress address) {
        var rrd = new Operation.Builder(READ_RESOURCE_DESCRIPTION, address)
                .param(INCLUDE_ALIASES, true)
                .param(OPERATIONS, true)
                .build();
        return execute(rrd);
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

    private ModelNode execute(Operation operation) {
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
}
