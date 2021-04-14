# Analyzer

Command line tool to parse and store the management model of a WildFly instance into a [graph](https://neo4j.com/) database. 

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

## Get Started

To analyse the management model you need a running WildFly and Neo4j 4.x instance. You can use a combination of the scripts in this repository to build and run WildFly and Neo4j instances as docker containers. Each script takes a WildFly version number >= 10 as parameter. The version number must be an integer and *not* the WildFly release number such as `23.0.0.Final`. 

- `build-wildfly.sh <version>`: Builds a WildFly image of the specified version, adds an admin user and exposes the management port as `99<version>`.
- `start-wildfly.sh <version>`: Runs the WildFly instance built by `build-wildfly.sh`.
- `start-neo4j.sh <version>`: Runs a Neo4j database and mounts the data directory to a temporary directory.
- `analyze.sh <version>`: Analyses the management model using the WildFly instance started by `start-wildfly.sh` and the Neo4j instance started by `start-neo4j.sh`.
- `build-modeldb.sh <version>`: Dumps the analysed Neo4j database into a single file and builds a new Neo4j image which imports the database dump at startup.
- `start-modeldb.sh <version>`: Runs the Neo4j instance built by `build-modeldb.sh`.
