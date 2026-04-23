package org.wildfly.modelgraph.analyzer.dmr;

import jakarta.json.JsonPointer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonModelTest {

    JsonModel model;

    @BeforeEach
    void beforeEach() {
        model = new JsonModel("{}", "{}");
    }

    @Test
    void empty() {
        ResourceAddress address = ResourceAddress.of("/");
        JsonPointer pointer = model.asPointer(address);
        assertEquals("", pointer.toString());
    }

    @Test
    void nested() {
        ResourceAddress address = ResourceAddress.of("/a=b/c=d/e=*/f=g");
        JsonPointer pointer = model.asPointer(address);
        assertEquals(
                "/children/a/model-description/b/children/c/model-description/d/children/e/model-description/*/children/f/model-description/g",
                pointer.toString());
    }

    @Test
    void realWorld() {
        String[][] tuples = {
                new String[]{"/subsystem=batch-jberet",
                        "/children/subsystem/model-description/batch-jberet"},
                new String[]{"/subsystem=batch-jberet/in-memory-job-repository=*",
                        "/children/subsystem/model-description/batch-jberet/children/in-memory-job-repository/model-description/*"},
                new String[]{"/core-service=management/access=audit/syslog-handler=*/protocol=udp",
                        "/children/core-service/model-description/management/children/access/model-description/audit/children/syslog-handler/model-description/*/children/protocol/model-description/udp"},
        };

        for (String[] tuple : tuples) {
            ResourceAddress address = ResourceAddress.of(tuple[0]);
            JsonPointer pointer = model.asPointer(address);
            assertEquals(tuple[1], pointer.toString());
        }
    }
}
