#!/bin/bash

# Starts a Neo4J model database
#
# Parameters
#   1. WildFly version >= 10


VERSION=$1
RELEASE=$VERSION.0.0.Final


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


docker run \
  --detach \
  --name=mgt-neo4j-$VERSION \
  --publish=74$VERSION:7474 --publish=76$VERSION:7687 \
  modelgraphtools/neo4j:$RELEASE
