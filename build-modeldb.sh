#!/bin/bash

# Builds the Neo4J model database
#
# Parameters
#   1. WildFly version >= 10


VERSION=$1
RELEASE=$VERSION.0.0.Final
NEO4J_VERSION=5.24.2
DATA_DIRECTORY=/tmp/mgt/data/$VERSION
DUMP_DIRECTORY=/tmp/mgt/dump/$VERSION


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


docker stop mgt-analyze-$VERSION 2>/dev/null
rm -rf $DUMP_DIRECTORY
mkdir -p $DUMP_DIRECTORY
docker run --interactive --tty --rm \
  --volume=$DATA_DIRECTORY:/data \
  --volume=$DUMP_DIRECTORY:/dump \
  --user="$(id -u):$(id -g)" \
  neo4j:${NEO4J_VERSION} \
  neo4j-admin database dump --to-path=/dump neo4j

cp src/main/docker/neo4j/mgt-entrypoint.sh $DUMP_DIRECTORY
docker build \
  --build-arg NEO4J_VERSION=$NEO4J_VERSION \
  --file src/main/docker/neo4j/Dockerfile \
  --tag quay.io/modelgraphtools/neo4j:$RELEASE \
  $DUMP_DIRECTORY
