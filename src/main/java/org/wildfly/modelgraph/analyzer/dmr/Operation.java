/*
 * Copyright 2015-2016 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.modelgraph.analyzer.dmr;

import org.jboss.dmr.ModelNode;

import static org.wildfly.modelgraph.analyzer.dmr.ModelDescriptionConstants.ADDRESS;
import static org.wildfly.modelgraph.analyzer.dmr.ModelDescriptionConstants.OP;

public class Operation extends ModelNode {

    public static class Builder {

        private final String name;
        private final ResourceAddress address;
        private final ModelNode parameter;

        public Builder(final String name, final ResourceAddress address) {
            this.address = address;
            this.name = name;
            this.parameter = new ModelNode();
        }

        public Builder param(String name, boolean value) {
            parameter.get(name).set(value);
            return this;
        }

        public Operation build() {
            return new Operation(name, address, parameter);
        }
    }


    private final String name;
    private final ResourceAddress address;
    private final ModelNode parameter;


    private Operation(String name, ResourceAddress address, ModelNode parameter) {
        this.name = name;
        this.address = address;
        this.parameter = parameter;
        init();
    }

    private void init() {
        set(parameter);
        get(OP).set(name);
        get(ADDRESS).set(address);
    }

    @Override
    public String toString() {
        return asCli();
    }

    String asCli() {
        var builder = new StringBuilder();
        if (address.isDefined() && !address.asList().isEmpty()) {
            builder.append(address);
        }
        builder.append(":").append(name);
        if (parameter.isDefined() && !parameter.asPropertyList().isEmpty()) {
            builder.append("(");
            for (var iterator = parameter.asPropertyList().iterator(); iterator.hasNext(); ) {
                var p = iterator.next();
                builder.append(p.getName()).append("=").append(p.getValue().asString());
                if (iterator.hasNext()) {
                    builder.append(",");
                }
            }
            builder.append(")");
        }
        return builder.toString();
    }
}
