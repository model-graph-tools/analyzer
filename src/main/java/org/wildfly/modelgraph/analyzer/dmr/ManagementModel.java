package org.wildfly.modelgraph.analyzer.dmr;

import java.util.List;

import org.jboss.dmr.ModelNode;

public interface ManagementModel extends AutoCloseable {

    Identity identity();

    List<String> children(ResourceAddress address);

    ModelNode resourceDescription(ResourceAddress address);
}
