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

## Graph Database

The analyzer walks through the resource tree of the management model until all resources are processed and stored in the graph database. The goal of the analysis is to prepare the data in such a way that makes it very easy to perform specific tasks, such as: 

- show a graphical representation of one or several resources
- show relationships between resources, its capabilities, operations and attributes
- find inconsistencies and identify weaknesses and opportunities for optimizations
- perform evaluations and analysis over a multitude of resources, attributes and operations
- find attributes which have been marked as deprecated since a given version

This is possible due to the way in which the information is stored in the database. The graph database consists of seven nodes and 18 relationships:

![Graph Database](https://model-graph-tools.github.io/img/graph.svg)

## Big Picture

The analyzer is part of the [model graph tools](https://model-graph-tools.github.io/) and creates the graph database used by the [model](https://github.com/model-graph-tools/model) service.

Take a look at the [setup](https://github.com/model-graph-tools/setup) repository how to get started.

<img src="https://model-graph-tools.github.io/img/tools.svg" alt="Model Graph Tools" width="512" />