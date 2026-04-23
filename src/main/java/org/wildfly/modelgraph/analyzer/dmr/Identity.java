package org.wildfly.modelgraph.analyzer.dmr;

import org.wildfly.modelgraph.analyzer.Version;

public record Identity(Type type,
                       String groupId, String artifactId, String name, String description,
                       Version version, Version managementVersion,
                       String url, String scmUrl, String[] licenses) {

    public enum Type {
        WILDFLY("wf"), FEATURE_PACK("fp");

        public final String id;

        Type(String id) {
            this.id = id;
        }
    }

    public static Identity wildFly(Version version, Version managementVersion) {
        return new Identity(Type.WILDFLY, "org.wildfly", "wildfly", "WildFly",
                "A powerful, modular, & lightweight application server that helps you build amazing applications.", version,
                managementVersion, "https://wildfly.org", "https://github.com/wildfly/wildfly", new String[]{"Apache-2.0"});
    }

    public static Identity featurePack(String groupId, String artifactId, String name, String description,
            Version version, String url, String scmUrl, String[] licenses) {
        return new Identity(Type.FEATURE_PACK, groupId, artifactId, name, description,
                version, Version.UNKNOWN, url, scmUrl, licenses);
    }

    public String identifier() {
        return String.format("%s:%s:%s", groupId, artifactId, version);
    }
}
