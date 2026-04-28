package org.wildfly.modelgraph.analyzer.neo4j;

import org.neo4j.driver.summary.SummaryCounters;

public interface GraphClient extends AutoCloseable {

    SummaryCounters execute(Cypher cypher);

    boolean exists(Cypher cypher);

    @Override
    void close();
}
