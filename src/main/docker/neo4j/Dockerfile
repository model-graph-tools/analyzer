ARG NEO4J_VERSION=5.26
FROM neo4j:${NEO4J_VERSION}

LABEL maintainer="Harald Pehl <hpehl@redhat.com>"

COPY --chown=neo4j load-dump.sh /load-dump.sh
COPY --chown=neo4j neo4j.dump /neo4j.dump
RUN chmod +x /load-dump.sh

ENV NEO4J_AUTH=none
ENV NEO4J_server_databases_default__to__read__only=true
ENV EXTENSION_SCRIPT=/load-dump.sh