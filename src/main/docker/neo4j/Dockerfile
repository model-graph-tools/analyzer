FROM neo4j:5.24.2

LABEL maintainer="Harald Pehl <hpehl@redhat.com>"

COPY --chown=neo4j db.dump db.dump
COPY --chown=neo4j mgt-entrypoint.sh mgt-entrypoint.sh
RUN chmod +x mgt-entrypoint.sh

ENV NEO4J_AUTH=neo4j/mgttgm
ENV NEO4J_dbms_databases_default__to__read__only=true
ENV NEO4J_dbms_security_auth__enabled=false
ENTRYPOINT ["./mgt-entrypoint.sh"]
