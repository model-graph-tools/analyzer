package org.wildfly.modelgraph.analyzer.neo4j;

import org.neo4j.driver.internal.summary.InternalSummaryCounters;
import org.neo4j.driver.summary.SummaryCounters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DryRunClient implements GraphClient {

    private static final Logger logger = LoggerFactory.getLogger(DryRunClient.class);

    public DryRunClient() {
        logger.info("Dry run mode: no Neo4j connection will be established");
    }

    @Override
    public SummaryCounters execute(Cypher cypher) {
        logger.info("[dry-run] {}", cypher.statement());
        logger.info("[dry-run] parameters: {}", cypher.parameters());
        return InternalSummaryCounters.EMPTY_STATS;
    }

    @Override
    public boolean exists(Cypher cypher) {
        logger.info("[dry-run] exists check: {}", cypher.statement());
        return false;
    }

    @Override
    public void close() {
        // nothing to close
    }
}
