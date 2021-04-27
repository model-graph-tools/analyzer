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
if ! [[ $VERSION =~ ^[0-9]+$ ]] ; then
  echo "Illegal version. Must be numeric and >= 10."
  exit 1
fi
if [[ "$VERSION" -lt "10" ]]; then
  echo "Illegal version. Must be numeric and >= 10."
  exit 1
fi


java -jar target/model-graph-analyzer-0.0.1.jar \
  --clean \
  --neo4j=localhost:66$VERSION \
  --wildfly=localhost:99$VERSION \
  --wildfly-user=admin \
  --wildfly-password=admin \
  /
