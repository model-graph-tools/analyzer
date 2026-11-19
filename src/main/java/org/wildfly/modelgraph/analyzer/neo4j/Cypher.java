package org.wildfly.modelgraph.analyzer.neo4j;

import java.util.HashMap;
import java.util.Map;

import org.neo4j.driver.Value;
import org.neo4j.driver.Values;

public class Cypher {

    private final StringBuilder builder;
    private final Map<String, Value> parameters;

    public Cypher(String cypher) {
        builder = new StringBuilder(cypher);
        parameters = new HashMap<>();
    }

    public Cypher comma() {
        builder.append(", ");
        return this;
    }

    public Cypher append(String cypher) {
        builder.append(cypher);
        return this;
    }

    public <T> Cypher append(String name, T value) {
        return append(name, name, value);
    }

    public <T> Cypher append(String attribute, String placeholder, T value) {
        var safePlaceHolder = placeholder.replace('-', '_');
        if (attribute.contains("-")) {
            builder.append("`");
        }
        builder.append(attribute);
        if (attribute.contains("-")) {
            builder.append("`");
        }
        builder.append(": $").append(safePlaceHolder);
        parameters.put(safePlaceHolder, Values.value(value));
        return this;
    }

    @Override
    public String toString() {
        return builder.toString();
    }

    String statement() {
        return builder.toString();
    }

    Value parameters() {
        return Values.value(parameters);
    }
}
