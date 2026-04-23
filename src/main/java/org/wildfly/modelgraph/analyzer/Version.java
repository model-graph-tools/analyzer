package org.wildfly.modelgraph.analyzer;

public record Version(int major, int minor, int patch) {

    public static final Version UNKNOWN = new Version(0, 0, 0);

    public static Version parse(String version) {
        if (version == null || version.isEmpty()) {
            throw new IllegalArgumentException("Version string cannot be null or empty");
        }

        // Extract numbers before any suffix (alpha, beta, final, etc)
        String[] parts = version.split("[^0-9.]")[0].split("\\.");
        if (parts.length < 3) {
            throw new IllegalArgumentException("Version must be in format 'major.minor.patch'");
        }

        try {
            return new Version(
                    Integer.parseInt(parts[0]),
                    Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2]));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid version format: " + version);
        }
    }
}
