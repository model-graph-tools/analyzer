#!/bin/bash

# Builds a Neo4J model database
#
# Parameters
#   1. WildFly version >= 10


VERSION=$1
RELEASE=$VERSION.0.0.Final
DATA_DIRECTORY=$PWD/target/data/$VERSION
DUMP_DIRECTORY=$PWD/target/dump/$VERSION


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


rm -rf $DUMP_DIRECTORY
mkdir -p $DUMP_DIRECTORY
docker stop neo4j-analyze-$VERSION 2>/dev/null
docker run --interactive --tty --rm \
  --volume=$DATA_DIRECTORY:/data \
  --volume=$DUMP_DIRECTORY:/dump \
  --user="$(id -u):$(id -g)" \
  neo4j \
  neo4j-admin dump --database=neo4j --to=/dump/db.dump

cp src/main/docker/neo4j/extension-script.sh $DUMP_DIRECTORY
docker build \
  --file src/main/docker/neo4j/Dockerfile \
  --tag modelgraphtools/neo4j:$RELEASE \
  $DUMP_DIRECTORY
