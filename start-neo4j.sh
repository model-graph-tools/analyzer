#!/bin/bash

# Starts a Neo4J instance with an empty database
#
# Parameters
#   1. WildFly version >= 10


VERSION=$1
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



mkdir -p $DATA_DIRECTORY
docker run \
  --detach \
  --name=neo4j-empty-$VERSION \
  --publish=64$VERSION:7474 --publish=66$VERSION:7687 \
  --volume=$DATA_DIRECTORY:/data \
  --env NEO4J_AUTH=none \
  neo4j
