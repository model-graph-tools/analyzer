#!/bin/bash

# Pushes the WildFly image to quay.io
#
# Parameters
#   1. WildFly version >= 10


VERSION=$1
RELEASE=$VERSION.0.0.Final


# This requires a valid configuration in ~/.docker/config.json
docker login quay.io
docker push quay.io/modelgraphtools/wildfly:$RELEASE