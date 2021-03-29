package org.wildfly.modelgraph.analyzer;

import java.util.concurrent.Callable;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.modelgraph.analyzer.dmr.WildFlyClient;
import org.wildfly.modelgraph.analyzer.neo4j.Neo4jClient;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@SuppressWarnings({"FieldCanBeLocal", "unused", "WeakerAccess"})
@Command(name = "model-graph-analyzer",
        sortOptions = false,
        descriptionHeading = "%n",
        parameterListHeading = "%nParameters:%n",
        optionListHeading = "%nOptions:%n",
        headerHeading = "%n",
        footerHeading = "%n",
        description = "Reads the management model from a WildFly instance and stores it as a graph in a Neo4j database",
        versionProvider = VersionProvider.class)
public class Main implements Callable<Stats> {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    @Option(names = {"-w", "--wildfly"},
            description = "WildFly instance as <server>[:<port>] with 9990 as default port. Omit to connect to a local WildFly instance at localhost:9990.")
    HostAndPort wildFly;

    @Option(names = {"-u", "--wildfly-user"}, description = "WildFly admin username")
    String wildFlyUsername = "admin";

    @Option(names = {"-p", "--wildfly-password"}, description = "WildFly admin password")
    String wildFlyPassword = "admin";

    @Option(names = {"-n", "--neo4j"},
            description = "Neo4j database as <server>[:<port>] with 7687 as default port. Omit to connect to a local Neo4j database at localhost:7687.")
    HostAndPort neo4j;

    @Option(names = {"-s", "--neo4j-user"}, description = "Neo4j username")
    String neo4jUsername = "neo4j";

    @Option(names = {"-t", "--neo4j-password"}, description = "Neo4j password")
    String neo4jPassword = "neo4j";

    @Option(names = {"-c", "--clean"},
            description = "remove all indexes, nodes, relationships and properties before analysing the management model tree.")
    boolean clean = false;

    @Option(names = {"-v", "--verbose"},
            description = "prints additional information about the processed resources.")
    boolean verbose = false;

    @Option(names = {"-V", "--version"}, versionHelp = true, description = "display version information and exit")
    boolean versionInfoRequested;

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "display this help message and exit")
    boolean helpRequested;

    @Parameters(paramLabel = "RESOURCE", description = "the root resource to analyse.")
    String resource = "/";

    public static void main(String[] args) {
        var cmd = new CommandLine(new Main());
        cmd.registerConverter(HostAndPort.class, HostAndPort::fromString);

        var exitCode = cmd.execute(args);
        Stats stats = cmd.getExecutionResult();
        if (stats != null) {
            logger.info("{}", stats);
        }
        System.exit(exitCode);
    }

    @Override
    public Stats call() {
        if (verbose) {
            var loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            var loggerList = loggerContext.getLoggerList();
            for (var l : loggerList) {
                l.setLevel(Level.DEBUG);
            }
        }

        try (var wc = new WildFlyClient(failSafeHostAndPort(wildFly, 9990), wildFlyUsername, wildFlyPassword);
             var nc = new Neo4jClient(failSafeHostAndPort(neo4j, 7687), neo4jUsername, neo4jPassword, clean)) {

            // start with resource and store metadata into neo4j database
            var analyzer = new Analyzer(wc, nc);
            analyzer.start(resource);
            return analyzer.stats();
        }
    }

    private HostAndPort failSafeHostAndPort(HostAndPort hostAndPort, int defaultPort) {
        HostAndPort safe;
        if (hostAndPort == null) {
            safe = HostAndPort.fromParts("localhost", defaultPort);
        } else if (!hostAndPort.hasPort()) {
            safe = HostAndPort.fromParts(hostAndPort.host(), defaultPort);
        } else {
            safe = hostAndPort;
        }
        return safe;
    }
}
