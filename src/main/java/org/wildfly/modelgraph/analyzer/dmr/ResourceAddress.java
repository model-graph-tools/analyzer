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

/** Represents a fully qualified DMR address ready to be put into a DMR operation. */
public class ResourceAddress extends ModelNode {

    public static ResourceAddress of(String address) {
        if (address != null && address.length() != 0 && !"/".equals(address)) {
            var node = new ModelNode();
            var normalized = address.startsWith("/") ? address.substring(1) : address;
            var segments = normalized.split("/");
            for (var segment : segments) {
                var kv = segment.split("=");
                node.add().set(kv[0], kv[1]);
            }
            return new ResourceAddress(node);
        }
        return new ResourceAddress();
    }

    private ResourceAddress() {
        setEmptyList();
    }

    private ResourceAddress(ModelNode address) {
        set(address);
    }

    public ResourceAddress add(String segment) {
        var address = new ResourceAddress(this);
        if (segment != null) {
            var kv = segment.split("=", 2);
            if (kv.length ==1) {
                address.add().set(kv[0], "*");
            } else if (kv.length == 2) {
                address.add().set(kv[0], kv[1]);
            }
        }
        return address;
    }

    public String getName() {
        if (size() == 0) {
            return "/";
        } else if (lastName() != null && lastValue() != null) {
            if ("*".equals(lastValue())) {
                return lastName();
            } else {
                return lastName() + "=" + lastValue();
            }
        } else {
            return "n/a";
        }
    }

    public int size() {
        return isDefined() ? asList().size() : 0;
    }

    public boolean isSingleton() {
        return !(size() == 0 || "*".equals(lastValue()));
    }

    @Override
    public String toString() {
        // Do not change implementation, it's used in neo4j!
        var builder = new StringBuilder();
        if (isDefined()) {
            builder.append("/");
            for (var iterator = asPropertyList().iterator(); iterator.hasNext(); ) {
                var segment = iterator.next();
                builder.append(segment.getName()).append("=").append(segment.getValue().asString());
                if (iterator.hasNext()) {
                    builder.append("/");
                }
            }
        } else {
            builder.append("n/a");
        }
        return builder.toString();
    }

    private String lastName() {
        var properties = asPropertyList();
        if (!properties.isEmpty()) {
            return properties.get(properties.size() - 1).getName();
        }
        return null;
    }

    private String lastValue() {
        var properties = asPropertyList();
        if (!properties.isEmpty()) {
            return properties.get(properties.size() - 1).getValue().asString();
        }
        return null;
    }
}
