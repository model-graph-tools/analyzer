package org.wildfly.modelgraph.analyzer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.joining;

class Stats {

    private long start;
    private Duration duration = Duration.ZERO;
    long resources;
    long failedResources;
    long attributes;
    long sensitive;
    long operations;
    long parameters;
    long capabilities;
    long relations;
    List<String> errors = new ArrayList<>();

    public void start() {
        start = System.nanoTime();
    }

    public void stop() {
        if (start != 0) {
            var elapsed = System.nanoTime() - start;
            duration = Duration.ofNanos(elapsed);
            start = 0;
        }
    }

    private String humanReadableDuration() {
        var s = duration.getSeconds();
        return String.format("%02d:%02d", (s % 3600) / 60, (s % 60));
    }

    @Override
    public String toString() {
        var result = String.format("Successfully created%n\t%,8d resources%n" +
                        "\t%,8d attributes%n" +
                        "\t%,8d sensitive constraints%n" +
                        "\t%,8d operations%n" +
                        "\t%,8d request properties%n" +
                        "\t%,8d capabilities and%n" +
                        "\t%,8d relationships",
                resources, attributes, sensitive, operations, parameters, capabilities, relations);
        if (failedResources > 0) {
            result += String.format("%n\t%,8d resources could not be processed.", failedResources);
        }
        result += String.format("%nin %s seconds.", humanReadableDuration());
        if (!errors.isEmpty()) {
            result += String.format("%n%nErrors%n%s", errors.stream().collect(joining(String.format("%n"))));
        }
        return result;
    }
}
