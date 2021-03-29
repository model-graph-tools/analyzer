package org.wildfly.modelgraph.analyzer.neo4j;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.exceptions.DatabaseException;
import org.neo4j.driver.summary.SummaryCounters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.modelgraph.analyzer.HostAndPort;

public class Neo4jClient implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(Neo4jClient.class);

    private final Driver driver;

    public Neo4jClient(HostAndPort hostAndPort, String username, String password, boolean clean) {
        var uri = "bolt://" + hostAndPort;
        var authToken = username != null && password != null
                ? AuthTokens.basic(username, password)
                : AuthTokens.none();
        driver = GraphDatabase.driver(uri, authToken);
        logger.info("Connected to Neo4j database at {}", hostAndPort);
        setup(clean);
    }

    private void setup(boolean clean) {
        if (clean) {
            try (var session = driver.session(); var tx = session.beginTransaction()) {
                var result = tx.run("MATCH (n) DETACH DELETE(n)");
                var summary = result.consume();
                logger.info("Removed {} nodes and {} relations",
                        summary.counters().nodesDeleted(),
                        summary.counters().relationshipsDeleted());
                tx.commit();
            }
            failSafeDrop("DROP INDEX ON :Parameter(name)");
            failSafeDrop("DROP INDEX ON :Operation(name)");
            failSafeDrop("DROP INDEX ON :Capability(name)");
            failSafeDrop("DROP INDEX ON :Resource(name)");
            failSafeDrop("DROP CONSTRAINT ON (r:Resource) ASSERT r.address IS UNIQUE");
            failSafeDrop("DROP INDEX ON :Attribute(name)");
        }
        try (var session = driver.session();
             var tx = session.beginTransaction()) {
            tx.run("CREATE INDEX ON :Resource(name)");
            tx.run("CREATE CONSTRAINT ON (r:Resource) ASSERT r.address IS UNIQUE");
            tx.run("CREATE INDEX ON :Attribute(name)");
            tx.run("CREATE INDEX ON :Capability(name)");
            tx.run("CREATE INDEX ON :Operation(name)");
            tx.run("CREATE INDEX ON :Parameter(name)");
            tx.commit();
        }
    }

    private void failSafeDrop(String statement) {
        try (var session = driver.session();
             var tx = session.beginTransaction()) {
            tx.run(statement);
            tx.commit();
        } catch (DatabaseException e) {
            logger.warn(e.getMessage());
        }
    }

    public SummaryCounters execute(Cypher cypher) {
        try (var session = driver.session();
             var tx = session.beginTransaction()) {

            logger.debug("Execute {} using {}", cypher.statement(), cypher.parameters());
            var result = tx.run(cypher.statement(), cypher.parameters());
            tx.commit();
            var counters = result.consume().counters();
            logger.debug("{} node and {} relations created", counters.nodesCreated(), counters.relationshipsCreated());
            return counters;
        }
    }

    @Override
    public void close() {
        logger.debug("Closing connection to Neo4j database");
        driver.close();
    }
}
