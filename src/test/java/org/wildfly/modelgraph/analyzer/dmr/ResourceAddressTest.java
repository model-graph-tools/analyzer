package org.wildfly.modelgraph.analyzer.dmr;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceAddressTest {

    @Test
    void of() {
        assertEquals("/", ResourceAddress.of(null).toString());
        assertEquals("/", ResourceAddress.of("").toString());
        assertEquals("/", ResourceAddress.of("/").toString());
        assertEquals("/foo=bar", ResourceAddress.of("/foo=bar").toString());
        assertEquals("/subsystem=datasources/data-source=*",
                ResourceAddress.of("/subsystem=datasources/data-source=*").toString());
    }

    @Test
    void add() {
        var a1 = ResourceAddress.of("/");
        var a2 = a1.add("foo=bar/baz=qux");
        assertNotSame(a1, a2);
        assertEquals("/", a1.toString());
        assertEquals("/foo=bar/baz=qux", a2.toString());
    }

    @Test
    void getName() {
        var a1 = ResourceAddress.of("/foo=*");
        var a2 = a1.add("foo=bar");
        assertEquals("foo", a1.getName());
        assertEquals("foo=bar", a2.getName());
    }

    @Test
    void isSingleton() {
        var a1 = ResourceAddress.of("/foo=*");
        var a2 = a1.add("foo=bar");
        assertFalse(a1.isSingleton());
        assertTrue(a2.isSingleton());
    }

    @Test
    void size() {
        assertEquals(0, ResourceAddress.of("/").size());
        assertEquals(1, ResourceAddress.of("/foo=bar").size());
        assertEquals(2, ResourceAddress.of("/foo=bar/baz=qux").size());
    }

}