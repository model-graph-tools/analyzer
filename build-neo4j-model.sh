#!/bin/bash

# Builds a Neo4J model database
#
# Parameters
#   1. WildFly version >= 10


VERSION=$1
RELEASE=$VERSION.0.0.Final
DATA_DIRECTORY=/tmp/mgt/data/$VERSION
DUMP_DIRECTORY=/tmp/mgt/dump/$VERSION
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
if ! [[ -d "$DATA_DIRECTORY" ]]
then
    echo "$DATA_DIRECTORY does not exists."
  exit 1
fi


docker stop mgta-neo4j-analyze-$VERSION 2>/dev/null
rm -rf $DUMP_DIRECTORY
mkdir -p $DUMP_DIRECTORY
docker run --interactive --tty --rm \
  --volume=$DATA_DIRECTORY:/data \
  --volume=$DUMP_DIRECTORY:/dump \
  --user="$(id -u):$(id -g)" \
  neo4j \
  neo4j-admin dump --database=neo4j --to=/dump/db.dump

cp src/main/docker/neo4j/mgt-entrypoint.sh $DUMP_DIRECTORY
docker build \
  --build-arg PLAY_URL=$PLAY_URL \
  --file src/main/docker/neo4j/Dockerfile \
  --tag modelgraphtools/neo4j:$RELEASE \
  $DUMP_DIRECTORY
