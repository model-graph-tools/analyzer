#!/bin/bash

# Analyse multiple WildFly versions
#
# Parameters
#   1. Multiple WildFly versions >= 10
#
# What it does (in pseudo code)
#
# for each version
#   ./build-wildfly.sh version
#   ./start-wildfly.sh version
#   ./start-neo4j.sh version
#   ./analyze.sh version
#   ./build-modeldb.sh version


# Prerequisites
if [[ "$#" -lt "1" ]]; then
  echo "Illegal number of parameters. Please use $0 <wildfly-versions>"
  exit 1
fi


for VERSION in "$@"
do
    ./build-wildfly.sh $VERSION
    ./start-wildfly.sh $VERSION
    ./start-neo4j.sh $VERSION
    sleep 300
    ./analyze.sh $VERSION
    ./build-modeldb.sh $VERSION
    docker stop mgt-wildfly-$VERSION 2>/dev/null
    docker rm mgt-wildfly-$VERSION 2>/dev/null
    docker rm mgt-analyze-$VERSION 2>/dev/null
done
