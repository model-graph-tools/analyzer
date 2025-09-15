package org.wildfly.modelgraph.analyzer.neo4j;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.exceptions.DatabaseException;
import org.neo4j.driver.summary.SummaryCounters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.modelgraph.analyzer.HostAndPort;
import org.wildfly.modelgraph.analyzer.Strings;

public class Neo4jClient implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(Neo4jClient.class);

    private final Driver driver;

    public Neo4jClient(HostAndPort hostAndPort, String username, String password, boolean clean, boolean append) {
        var uri = "bolt://" + hostAndPort;
        var authToken = !Strings.isEmpty(username) && !Strings.isEmpty(password)
                ? AuthTokens.basic(username, password)
                : AuthTokens.none();
        driver = GraphDatabase.driver(uri, authToken);
        logger.info("Connected to Neo4j database at {}", hostAndPort);
        setup(clean, append);
    }

    private void setup(boolean clean, boolean append) {
        if (clean) {
            try (var session = driver.session(); var tx = session.beginTransaction()) {
                var result = tx.run("MATCH (n) DETACH DELETE(n)");
                var summary = result.consume();
                logger.info("Removed {} nodes and {} relations",
                        summary.counters().nodesDeleted(),
                        summary.counters().relationshipsDeleted());
                tx.commit();
            }
            failSafeDrop("DROP INDEX parameter_name IF EXISTS");
            failSafeDrop("DROP INDEX operation_name IF EXISTS");
            failSafeDrop("DROP INDEX capability_name IF EXISTS");
            failSafeDrop("DROP INDEX resource_name IF EXISTS");
            failSafeDrop("DROP CONSTRAINT unique_address IF EXISTS");
            failSafeDrop("DROP INDEX attribute_name IF EXISTS");
        }
        if (!append) {
            try (var session = driver.session();
                 var tx = session.beginTransaction()) {
                tx.run("CREATE INDEX resource_name FOR (r:Resource) ON (r.name)");
                tx.run("CREATE CONSTRAINT unique_address FOR (r:Resource) REQUIRE r.address IS UNIQUE");
                tx.run("CREATE INDEX attribute_name FOR (a:Attribute) ON (a.name)");
                tx.run("CREATE INDEX capability_name FOR (c:Capability) ON (c.name)");
                tx.run("CREATE INDEX operation_name FOR (o:Operation) ON (o.name)");
                tx.run("CREATE INDEX parameter_name FOR (p:Parameter) ON (p.name)");
                tx.commit();
            }
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

    public boolean exists(Cypher cypher) {
        cypher.append(" RETURN count(r) AS node_count");
        try (var session = driver.session()) {
            long count = session.executeRead(tx -> {
                var result = tx.run(cypher.statement(), cypher.parameters());
                return result.single().get("node_count").asLong();
            });
            return count > 0;
        }
    }

    @Override
    public void close() {
        logger.debug("Closing connection to Neo4j database");
        driver.close();
    }
}
