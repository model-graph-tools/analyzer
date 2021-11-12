ARG WILDFLY_RELEASE=latest
ARG DOCKER_REPO=jboss/wildfly
FROM ${DOCKER_REPO}:${WILDFLY_RELEASE}

LABEL maintainer="Harald Pehl <hpehl@redhat.com>"

RUN /opt/jboss/wildfly/bin/add-user.sh -u admin -p admin --silent
CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0", "-bmanagement", "0.0.0.0", "-c", "standalone-full-ha.xml"]
