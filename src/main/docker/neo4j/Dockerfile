FROM neo4j

LABEL maintainer="Harald Pehl <hpehl@redhat.com>"

COPY db.dump /db.dump
COPY extension-script.sh /extension-script.sh
ENV EXTENSION_SCRIPT=/extension-script.sh
