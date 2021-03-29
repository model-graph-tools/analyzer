#!/bin/bash

# Runs the analyzer
#
# Parameters
#   1. WildFly version >= 10


VERSION=$1


# Prerequisites
if [[ "$#" -ne 1 ]]; then
  echo "Illegal number of parameters. Please use $0 <wildfly-version>"
  exit 1
fi

java -jar target/model-graph-analyzer-0.0.1.jar \
  --clean \
  --neo4j=localhost:76$VERSION \
  --neo4j-user=neo4j \
  --neo4j-password=neo5j \
  --wildfly=localhost:99$VERSION \
  --wildfly-user=admin \
  --wildfly-password=admin \
  /
