# Analyzer

Command line tool to parse and store the management model of a WildFly instance into a [graph](https://neo4j.com/) database. 

## Get Started

To analyse the management model tree you need a running WildFly and Neo4j 4.x instance. To install Neo4j, download it from https://neo4j.com/download/ or use `brew install neo4j` if you're on a Mac. Start Neo4j using `neo4j start` from the command line and open [http://localhost:7474/](http://localhost:7474/). If you login for the first time, you have to change your password. To change it back to the default use 

```cypher
CALL dbms.changePassword('neo4j')
```

and refresh your browser.

## Usage

The command line tool accepts the following options and parameters:

```
Usage: model-graph-analyzer [-chvV] [-n=<neo4j>] [-p=<wildFlyPassword>]
                            [-s=<neo4jUsername>] [-t=<neo4jPassword>]
                            [-u=<wildFlyUsername>] [-w=<wildFly>] RESOURCE

Reads the management model from a WildFly instance and stores it as a graph in
a Neo4j database

Parameters:
      RESOURCE              the root resource to analyse.

Options:
  -w, --wildfly=<wildFly>   WildFly instance as <server>[:<port>] with 9990 as
                              default port. Omit to connect to a local WildFly
                              instance at localhost:9990.
  -u, --wildfly-user=<wildFlyUsername>
                            WildFly admin username
  -p, --wildfly-password=<wildFlyPassword>
                            WildFly admin password
  -n, --neo4j=<neo4j>       Neo4j database as <server>[:<port>] with 7687 as
                              default port. Omit to connect to a local Neo4j
                              database at localhost:7687.
  -s, --neo4j-user=<neo4jUsername>
                            Neo4j username
  -t, --neo4j-password=<neo4jPassword>
                            Neo4j password
  -c, --clean               remove all indexes, nodes, relationships and
                              properties before analysing the management model
                              tree.
  -v, --verbose             prints additional information about the processed
                              resources.
  -V, --version             display version information and exit
  -h, --help                display this help message and exit
```

If everything runs locally using the default ports, use the following command to analyses the complete resource tree:

```bash
java -jar model-graph-analyzer-0.2.0.jar /
    --wildfly-user=admin --wildfly-password=admin \
    --neo4j-user=neo4j --neo4j-password=neo4j /
```

This will populate the Neo4j instance with nodes, relations and properties of the specified resource (sub)tree. Please make sure the Neo4j instance is empty or use the `--clean` option to remove existing data. After the tool has finished, head to [http://localhost:7474/](http://localhost:7474/) and enter some queries. 
