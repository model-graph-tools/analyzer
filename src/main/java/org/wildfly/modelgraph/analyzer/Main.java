package org.wildfly.modelgraph.analyzer;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.modelgraph.analyzer.dmr.JsonModel;
import org.wildfly.modelgraph.analyzer.dmr.ManagementModel;
import org.wildfly.modelgraph.analyzer.dmr.WildFlyInstance;
import org.wildfly.modelgraph.analyzer.neo4j.DryRunClient;
import org.wildfly.modelgraph.analyzer.neo4j.GraphClient;
import org.wildfly.modelgraph.analyzer.neo4j.Neo4jClient;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
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
        description = "Reads the management model from a WildFly instance or feature pack and stores it as a graph in a Neo4j database",
        versionProvider = VersionProvider.class)
public class Main implements Callable<Stats> {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    static class WildFly {

        @Option(names = {"-w", "--wildfly"},
                description = "WildFly instance as <server>[:<port>] with 9990 as default port. Omit to connect to a local WildFly instance at localhost:9990.")
        HostAndPort host;

        @Option(names = {"-u", "--wildfly-user"}, description = "WildFly admin username")
        String username = "";

        @Option(names = {"-p", "--wildfly-password"}, description = "WildFly admin password")
        String password = "";
    }

    static class DocZip {

        @Option(names = {"-z", "--doc-zip"}, description = "Documentation classifier of a WildFly server or feature pack")
        String filename;
    }

    static class Source {

        @ArgGroup(exclusive = false, multiplicity = "1")
        WildFly wildFly;

        @ArgGroup(exclusive = false, multiplicity = "1")
        DocZip docZip;
    }

    @ArgGroup(multiplicity = "1")
    Source source;

    @Option(names = {"-n", "--neo4j"},
            description = "Neo4j database as <server>[:<port>] with 7687 as default port. Omit to connect to a local Neo4j database at localhost:7687.")
    HostAndPort neo4jHost;

    @Option(names = {"-s", "--neo4j-user"}, description = "Neo4j username")
    String neo4jUsername = "";

    @Option(names = {"-t", "--neo4j-password"}, description = "Neo4j password")
    String neo4jPassword = "";

    @Option(names = {"-c", "--clean"},
            description = "Remove all indexes, nodes, relationships and properties before analyzing the management model tree.")
    boolean clean = false;

    @Option(names = {"-a", "--append"},
            description = "Only add new resources, existing resources will be skipped.")
    boolean append = false;

    @Option(names = {"-d", "--dry-run"},
            description = "Analyze the source without writing to Neo4j. Logs Cypher statements that would be executed.")
    boolean dryRun = false;

    @Option(names = {"-v", "--verbose"},
            description = "Prints additional information about the processed resources.")
    boolean verbose = false;

    @Option(names = {"-V", "--version"}, versionHelp = true, description = "Display version information and exit")
    boolean versionInfoRequested;

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "Display this help message and exit")
    boolean helpRequested;

    @Parameters(paramLabel = "RESOURCE", defaultValue = "/", arity = "0..1",
            description = "the root resource to analyze. Defaults to '/' (entire management model tree).")
    String resource = "/";

    static void main(String[] args) {
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
        if (dryRun && (clean || append)) {
            logger.warn("Dry run mode: --clean and --append options are ignored");
        }
        try (var mm = createManagementModel();
             var nc = createGraphClient()) {
            var analyzer = new Analyzer(mm, nc);
            analyzer.start(resource, append);
            return analyzer.stats();
        } catch (Exception e) {
            logger.error("Analyzer failed: {}", e.getMessage());
            return null;
        }
    }

    private GraphClient createGraphClient() {
        if (dryRun) {
            return new DryRunClient();
        }
        return new Neo4jClient(failSafeHostAndPort(neo4jHost, 7687), neo4jUsername, neo4jPassword, clean, append);
    }

    private ManagementModel createManagementModel() {
        if (source.wildFly != null) {
            return new WildFlyInstance(failSafeHostAndPort(source.wildFly.host, 9990), source.wildFly.username,
                    source.wildFly.password);
        } else if (source.docZip != null) {
            return new JsonModel(source.docZip.filename);
        } else {
            throw new RuntimeException("No WildFly instance or documentation classifier specified");
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
