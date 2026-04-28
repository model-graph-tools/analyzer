package org.wildfly.modelgraph.analyzer.neo4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.neo4j.driver.internal.summary.InternalSummaryCounters;
import org.neo4j.driver.summary.SummaryCounters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DryRunClient implements GraphClient {

    private static final Logger logger = LoggerFactory.getLogger(DryRunClient.class);
    private static final Pattern NODE_PATTERN = Pattern.compile(
            "(?:CREATE|MERGE)\\s+(?:\\(\\w*\\)\\s*-\\[.*?\\]->\\s*)?\\(\\w*:\\w+");
    private static final Pattern RELATIONSHIP_PATTERN = Pattern.compile(
            "MERGE\\s+(?:\\(\\w+\\)\\s*)?-\\[:");

    public DryRunClient() {
        logger.info("Dry run mode: no Neo4j connection will be established");
    }

    @Override
    public SummaryCounters execute(Cypher cypher) {
        logger.debug("{}", cypher.statement());
        logger.debug("parameters: {}", cypher.parameters());
        return estimateCounters(cypher.statement());
    }

    @Override
    public boolean exists(Cypher cypher) {
        logger.debug("exists check: {}", cypher.statement());
        return false;
    }

    @Override
    public void close() {
        // nothing to close
    }

    private SummaryCounters estimateCounters(String statement) {
        int nodes = countNodes(statement);
        int relationships = countRelationships(statement);
        if (nodes == 0 && relationships == 0) {
            return InternalSummaryCounters.EMPTY_STATS;
        }
        return new InternalSummaryCounters(nodes, 0, relationships, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    private int countNodes(String statement) {
        int count = 0;
        Matcher m = NODE_PATTERN.matcher(statement);
        while (m.find()) {
            count++;
        }
        return count;
    }

    private int countRelationships(String statement) {
        int count = 0;
        Matcher m = RELATIONSHIP_PATTERN.matcher(statement);
        while (m.find()) {
            count++;
        }
        return count;
    }
}
