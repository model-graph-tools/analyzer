#!/bin/bash

# Pushes multiple images
#
# Parameters
#   1. Multiple WildFly versions >= 10
#
# What it does (in pseudo code)
#
# for each version
#   ./push-modeldb.sh version
#   ./push-wildfly.sh version


# Prerequisites
if [[ "$#" -lt "1" ]]; then
  echo "Illegal number of parameters. Please use $0 <wildfly-versions>"
  exit 1
fi


for VERSION in "$@"
do
    ./push-modeldb.sh $VERSION
    ./push-wildfly.sh $VERSION
done
