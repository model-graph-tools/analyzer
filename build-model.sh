#!/bin/bash

# Builds a Neo4J model database
#
# Parameters
#   1. WildFly version >= 10


VERSION=$1
RELEASE=$VERSION.0.0.Final
DATA_DIRECTORY=/tmp/mgt/$VERSION


# Prerequisites
if [[ "$#" -ne 1 ]]; then
  echo "Illegal number of parameters. Please use $0 <wildfly-version>"
  exit 1
fi
if ! [[ $VERSION =~ ^[0-9]+$ ]] ; then
  echo "Illegal version. Must be numeric and >= 10."
  exit 1
fi
if [[ "$VERSION" -lt "10" ]]; then
  echo "Illegal version. Must be numeric and >= 10."
  exit 1
fi
if ! [[ -d "$DATA_DIRECTORY" ]]
then
    echo "$DATA_DIRECTORY does not exists."
  exit 1
fi


docker build \
  --file src/main/docker/neo4j/Dockerfile \
  --tag modelgraphtools/neo4j:$RELEASE \
  $DATA_DIRECTORY
