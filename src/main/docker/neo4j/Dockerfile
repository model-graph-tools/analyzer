ARG NEO4J_VERSION=latest
FROM neo4j:${NEO4J_VERSION}

LABEL maintainer="Harald Pehl <hpehl@redhat.com>"

COPY --chown=neo4j neo4j.dump neo4j.dump
COPY --chown=neo4j mgt-entrypoint.sh mgt-entrypoint.sh
RUN chmod +x mgt-entrypoint.sh

ENV NEO4J_dbms_databases_default__to__read__only=true
ENV NEO4J_dbms_security_auth__enabled=false
ENTRYPOINT ["./mgt-entrypoint.sh"]