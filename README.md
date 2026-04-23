# Analyzer

Command line tool to read the management model of a WildFly instance or feature pack and store it as a graph in a [Neo4j](https://neo4j.com/) database.

## Data Sources

The analyzer supports two data sources (mutually exclusive):

- **WildFly instance** (`-w`) -- connects to a running WildFly server via the native management protocol (default port 9990)
- **Documentation ZIP** (`-z`) -- reads from a Galleon doc-zip artifact containing the management model as JSON

## Usage

```
Usage: model-graph-analyzer [-achvV] [-n=<neo4j>] [-s=<neo4jUsername>]
                            [-t=<neo4jPassword>] (-w=<wildFly> [-u=<wildFlyUsername>]
                            [-p=<wildFlyPassword>]) | (-z=<filename>) RESOURCE

Reads the management model from a WildFly instance or feature pack and stores
it as a graph in a Neo4j database

Parameters:
      RESOURCE                the root resource to analyze.

Source (one required):
  -w, --wildfly=<wildFly>     WildFly instance as <server>[:<port>] with 9990 as default port.
  -u, --wildfly-user=<wildFlyUsername>
                              WildFly admin username
  -p, --wildfly-password=<wildFlyPassword>
                              WildFly admin password
  -z, --doc-zip=<filename>    Documentation classifier of a WildFly server or feature pack

Neo4j:
  -n, --neo4j=<neo4j>         Neo4j database as <server>[:<port>] with 7687 as default port. 
                              Omit to connect to a local Neo4j database at localhost:7687.
  -s, --neo4j-user=<neo4jUsername>
                              Neo4j username
  -t, --neo4j-password=<neo4jPassword>
                              Neo4j password

Options:
  -a, --append                Only add new resources, existing resources will be skipped.
  -c, --clean                 Remove all indexes, nodes, relationships and properties before 
                              analyzing the management model tree.
  -v, --verbose               Prints additional information about the processed resources.
  -V, --version               Display version information and exit
  -h, --help                  Display this help message and exit
```

## Graph Database

The analyzer walks through the resource tree of the management model until all resources are processed and stored in the
graph database. The goal of the analysis is to prepare the data in such a way that makes it straightforward to perform
specific tasks, such as:

- show a graphical representation of one or several resources
- show relationships between resources, its capabilities, operations and attributes
- find inconsistencies and identify weaknesses and opportunities for optimizations
- perform evaluations and analysis over a multitude of resources, attributes and operations
- find attributes which have been marked as deprecated since a given version

This is possible due to the way in which the information is stored in the database. The graph database consists of these
nodes and relationships:

![Graph Database](https://model-graph-tools.github.io/img/graph.svg)
