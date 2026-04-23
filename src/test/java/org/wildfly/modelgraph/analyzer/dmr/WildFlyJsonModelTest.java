package org.wildfly.modelgraph.analyzer.dmr;

import java.util.List;

import org.jboss.dmr.ModelNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.wildfly.modelgraph.analyzer.dmr.JsonModelReader.read;
import static org.wildfly.modelgraph.analyzer.dmr.ModelDescriptionConstants.ATTRIBUTES;

class WildFlyJsonModelTest implements JsonModelReader {

    JsonModel model;

    @BeforeEach
    void beforeEach() {
        model = new JsonModel(
                read("/wildfly-galleon-pack-39.0.1.Final-doc/META-INF/metadata.json"),
                read("/wildfly-galleon-pack-39.0.1.Final-doc/META-INF/management-api.json"));
    }

    @Test
    void identity() {
        Identity identity = model.identity();
        assertEquals("org.wildfly", identity.groupId());
        assertEquals("wildfly-galleon-pack", identity.artifactId());
        assertEquals(39, identity.version().major());
        assertEquals(0, identity.version().minor());
        assertEquals(1, identity.version().patch());
    }

    @Test
    void childrenOfRoot() {
        List<String> children = model.children(ResourceAddress.of("/"));
        // wildcard types
        assertTrue(children.contains("deployment"));
        assertTrue(children.contains("path"));
        assertTrue(children.contains("extension"));
        assertTrue(children.contains("interface"));
        assertTrue(children.contains("system-property"));
        // singleton types
        assertTrue(children.stream().anyMatch(c -> c.startsWith("subsystem=")));
        assertTrue(children.stream().anyMatch(c -> c.startsWith("core-service=")));
        assertTrue(children.contains("socket-binding-group"));
    }

    @Test
    void childrenOfUndertow() {
        List<String> children = model.children(ResourceAddress.of("/subsystem=undertow"));
        // wildcard children
        assertTrue(children.contains("byte-buffer-pool"));
        assertTrue(children.contains("server"));
        assertTrue(children.contains("servlet-container"));
        // singleton children
        assertTrue(children.contains("configuration=filter"));
        assertTrue(children.contains("configuration=handler"));
    }

    @Test
    void childrenOfLeafResource() {
        List<String> children = model.children(ResourceAddress.of("/subsystem=batch-jberet"));
        assertFalse(children.isEmpty());
        assertTrue(children.contains("in-memory-job-repository"));
        assertTrue(children.contains("thread-pool"));
    }

    @Test
    void resourceDescription() {
        ResourceAddress address = ResourceAddress.of("/subsystem=undertow");
        ModelNode resource = model.resourceDescription(address);
        assertTrue(resource.hasDefined(ATTRIBUTES));
    }
}
