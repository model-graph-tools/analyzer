FROM neo4j

LABEL maintainer="Harald Pehl <hpehl@redhat.com>"

COPY --chown=neo4j db.dump db.dump
COPY --chown=neo4j mgt-entrypoint.sh mgt-entrypoint.sh
RUN chmod +x mgt-entrypoint.sh

ENV NEO4J_AUTH=neo4j/mgttgm
ENV NEO4J_dbms_read__only=true
ENTRYPOINT ["./mgt-entrypoint.sh"]
