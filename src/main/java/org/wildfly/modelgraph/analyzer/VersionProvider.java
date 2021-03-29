package org.wildfly.modelgraph.analyzer;

import picocli.CommandLine;

import java.io.IOException;
import java.util.Properties;

public class VersionProvider implements CommandLine.IVersionProvider {

    @Override
    public String[] getVersion() throws IOException {
        var url = getClass().getResource("/version.properties");
        if (url == null) {
            return new String[] {"No version.properties file found in the classpath."};
        }
        var properties = new Properties();
        properties.load(url.openStream());
        return new String[]{
                "Model Graph Analyzer " + properties.getProperty("version", "n/a"),
                "Build " + properties.getProperty("build.date", "n/a"),
                properties.getProperty("build.url", "n/a")
        };
    }
}
