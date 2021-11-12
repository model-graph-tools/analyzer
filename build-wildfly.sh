#!/bin/bash

# Builds the WildFly image to be analyzed
#
# Parameters
#   1. WildFly version >= 10


VERSION=$1
RELEASE=$VERSION.0.0.Final
REPO=quay.io/wildfly/wildfly


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


# Starting with WildFly 24, we use quay.io
# for the WildFly images.
if [[ "$VERSION" -lt "24" ]]; then
  REPO=jboss/wildfly
fi


docker build \
  --build-arg WILDFLY_RELEASE=$RELEASE \
  --build-arg DOCKER_REPO=$REPO \
  --file src/main/docker/wildfly/Dockerfile \
  --tag quay.io/modelgraphtools/wildfly:$RELEASE \
  src/main/docker/wildfly
