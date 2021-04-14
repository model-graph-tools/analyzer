#!/bin/bash

# Starts a Neo4J model database
#
# Parameters
#   1. WildFly version >= 10


VERSION=$1
RELEASE=$VERSION.0.0.Final
PLAY_URL=https://model-graph-tools.github.io/play/$VERSION/


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
  --name=neo4j-model-$VERSION \
  --publish=74$VERSION:7474 --publish=76$VERSION:7687 \
  --env NEO4J_browser_post__connect__cmd="play $PLAY_URL" \
  modelgraphtools/neo4j:$RELEASE
