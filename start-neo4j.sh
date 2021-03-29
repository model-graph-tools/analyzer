#!/bin/bash

# Starts a Neo4J database
#
# Parameters
#   1. WildFly version >= 10


VERSION=$1


# Prerequisites
if [[ "$#" -ne 1 ]]; then
  echo "Illegal number of parameters. Please use $0 <wildfly-version>"
  exit 1
fi

docker run \
  --detach \
  --name=neo$VERSION \
  --publish=74$VERSION:7474 --publish=76$VERSION:7687 \
  --volume=$HOME/dev/wildfly/model-graph/analyzer/data/wf$VERSION:/data \
  --env NEO4J_AUTH=neo4j/neo5j \
  neo4j
