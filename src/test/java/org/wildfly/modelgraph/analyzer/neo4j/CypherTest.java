package org.wildfly.modelgraph.analyzer.neo4j;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CypherTest {

    @Test
    void simple() {
        assertEquals("foo", new Cypher("foo").statement());
    }

    @Test
    void append() {
        var cypher = new Cypher("foo").append("-bar");
        assertEquals("foo-bar", cypher.statement());
        assertTrue(cypher.parameters().isEmpty());
    }

    @Test
    void variable() {
        var cypher = new Cypher("CREATE (:Foo {")
                .append("name", "foo")
                .append("})");
        var parameters = cypher.parameters();
        assertEquals("CREATE (:Foo {name: $name})", cypher.statement());
        assertEquals(1, parameters.size());
        assertEquals("foo", parameters.get("name").asString());
    }

    @Test
    void placeholder() {
        var cypher = new Cypher("CREATE (:Foo {")
                .append("name", "bar", "foo")
                .append("})");
        var parameters = cypher.parameters();
        assertEquals("CREATE (:Foo {name: $bar})", cypher.statement());
        assertEquals(1, parameters.size());
        assertEquals("foo", parameters.get("bar").asString());
    }

    @Test
    void backtick() {
        var cypher = new Cypher("CREATE (:Foo {")
                .append("foo-bar", "bar-foo")
                .append("})");
        var parameters = cypher.parameters();
        assertEquals("CREATE (:Foo {`foo-bar`: $foo_bar})", cypher.statement());
        assertEquals(1, parameters.size());
        assertEquals("bar-foo", parameters.get("foo_bar").asString());
    }
}