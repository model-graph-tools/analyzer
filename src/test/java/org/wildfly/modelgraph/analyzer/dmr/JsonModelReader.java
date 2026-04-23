package org.wildfly.modelgraph.analyzer.dmr;

import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;

interface JsonModelReader {

    static String read(String fileName) {
        try (java.io.InputStream is = JsonModelReader.class.getResourceAsStream(fileName)) {
            if (is == null) {
                throw new RuntimeException("Resource not found: " + fileName);
            }
            return new String(is.readAllBytes(), UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read resource " + fileName + ": " + e.getMessage(), e);
        }
    }
}
