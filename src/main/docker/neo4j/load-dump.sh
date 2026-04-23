#!/bin/bash

neo4j-admin database load --from-path=/ --overwrite-destination=true neo4j
