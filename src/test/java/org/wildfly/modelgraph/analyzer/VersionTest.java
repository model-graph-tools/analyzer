package org.wildfly.modelgraph.analyzer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VersionTest {

    @Test
    void valid() {
        String versionString = "1.2.3";
        Version version = Version.parse(versionString);
        assertNotNull(version);
        assertEquals(1, version.major());
        assertEquals(2, version.minor());
        assertEquals(3, version.patch());
    }

    @Test
    void withSuffix() {
        String versionString = "38.0.0.Beta1-SNAPSHOT";
        Version version = Version.parse(versionString);
        assertNotNull(version);
        assertEquals(38, version.major());
        assertEquals(0, version.minor());
        assertEquals(0, version.patch());
    }

    @Test
    void tooManyParts() {
        String versionString = "1.2.3.4";
        Version version = Version.parse(versionString);

        // Assert
        assertNotNull(version);
        assertEquals(1, version.major());
        assertEquals(2, version.minor());
        assertEquals(3, version.patch());
    }

    @Test
    void nil() {
        String versionString = null;
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> Version.parse(versionString));
        assertEquals("Version string cannot be null or empty", exception.getMessage());
    }

    @Test
    void empty() {
        String versionString = "";
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> Version.parse(versionString));
        assertEquals("Version string cannot be null or empty", exception.getMessage());
    }

    @Test
    void incomplete() {
        String versionString = "1.2";
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> Version.parse(versionString));
        assertEquals("Version must be in format 'major.minor.patch'", exception.getMessage());
    }

    @Test
    void invalid() {
        String versionString = "1.a.3";
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> Version.parse(versionString));
        assertEquals("Version must be in format 'major.minor.patch'", exception.getMessage());
    }
}