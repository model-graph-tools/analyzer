FROM neo4j

ARG PLAY_URL
LABEL maintainer="Harald Pehl <hpehl@redhat.com>"

COPY --chown=neo4j db.dump db.dump
COPY --chown=neo4j mgt-entrypoint.sh mgt-entrypoint.sh
RUN chmod +x mgt-entrypoint.sh

ENV NEO4J_AUTH=neo4j/mgttgm
ENV NEO4J_dbms_read__only=true
ENV NEO4J_browser_post__connect__cmd="play $PLAY_URL"
ENTRYPOINT ["./mgt-entrypoint.sh"]
