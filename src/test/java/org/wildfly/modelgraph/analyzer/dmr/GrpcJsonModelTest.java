package org.wildfly.modelgraph.analyzer.dmr;

import java.util.List;

import org.jboss.dmr.ModelNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.wildfly.modelgraph.analyzer.dmr.JsonModelReader.read;
import static org.wildfly.modelgraph.analyzer.dmr.ModelDescriptionConstants.ATTRIBUTES;
import static org.wildfly.modelgraph.analyzer.dmr.ModelDescriptionConstants.UNIT;

class GrpcJsonModelTest implements JsonModelReader {

    JsonModel model;

    @BeforeEach
    void beforeEach() {
        model = new JsonModel(
                read("/wildfly-grpc-preview-feature-pack-0.1.16.Final-doc/META-INF/metadata.json"),
                read("/wildfly-grpc-preview-feature-pack-0.1.16.Final-doc/META-INF/management-api.json"));
    }

    @Test
    void identity() {
        Identity identity = model.identity();
        assertEquals("org.wildfly.extras.grpc", identity.groupId());
        assertEquals("wildfly-grpc-preview-feature-pack", identity.artifactId());
        assertEquals(0, identity.version().major());
        assertEquals(1, identity.version().minor());
        assertEquals(16, identity.version().patch());
    }

    @Test
    void resourceDescription() {
        ResourceAddress address = ResourceAddress.of("/subsystem=grpc");
        ModelNode resource = model.resourceDescription(address);
        assertEquals("SECONDS", resource.get(ATTRIBUTES).get("max-connection-idle").get(UNIT).asString());
    }

    @Test
    void childrenOfRoot() {
        List<String> children = model.children(ResourceAddress.of("/"));
        assertTrue(children.contains("subsystem=grpc"));
        assertTrue(children.contains("deployment"));
    }

    @Test
    void childrenOfSubsystem() {
        List<String> children = model.children(ResourceAddress.of("/subsystem=grpc"));
        assertTrue(children.isEmpty());
    }

    @Test
    void childrenOfNonExistent() {
        List<String> children = model.children(ResourceAddress.of("/subsystem=nonexistent"));
        assertTrue(children.isEmpty());
    }
}
