#!/bin/bash

# Starts a WildFly server
#
# Parameters
#   1. WildFly version >= 10


VERSION=$1


# Prerequisites
if [[ "$#" -ne 1 ]]; then
  echo "Illegal number of parameters. Please use $0 <wildfly-version>"
  exit 1
fi

docker run \
  --detach \
  --name=wf$VERSION \
  --publish=99$VERSION:9990 \
  jboss/wildfly:$VERSION.0.0.Final \
  /opt/jboss/wildfly/bin/standalone.sh \
  -b 0.0.0.0 \
  -bmanagement 0.0.0.0 \
  -c standalone-full-ha.xml
