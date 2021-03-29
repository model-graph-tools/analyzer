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

/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.wildfly.modelgraph.analyzer.dmr;

/** String constants frequently used in model descriptions and DMR operations. */
public interface ModelDescriptionConstants {

    // KEEP THESE IN ALPHABETICAL ORDER!
    String ACCESS_CONSTRAINTS = "access-constraints";
    String ACCESS_TYPE = "access-type";
    String ADDRESS = "address";
    String ALIAS = "alias";
    String ALTERNATIVES = "alternatives";
    String ATTRIBUTE_GROUP = "attribute-group";
    String ATTRIBUTES = "attributes";
    String ATTRIBUTES_ONLY = "attributes-only";

    String CAPABILITIES = "capabilities";
    String CAPABILITY_REFERENCE = "capability-reference";
    String CHILD = "child";
    String CHILDREN = "children";
    String CHILD_DESCRIPTIONS = "child-descriptions";

    String DEFAULT = "default";
    String DEPRECATED = "deprecated";
    String DESCRIPTION = "description";

    String EXPRESSIONS_ALLOWED = "expressions-allowed";

    String FAILED = "failed";
    String FAILURE_DESCRIPTION = "failure-description";

    String GLOBAL = "global";

    String INCLUDE_ALIASES = "include-aliases";
    String INCLUDE_RUNTIME = "include-runtime";
    String INCLUDE_SINGLETONS = "include-singletons";

    String LIST_ADD = "list-add";
    String LIST_CLEAR = "list-clear";
    String LIST_GET = "list-get";
    String LIST_REMOVE = "list-remove";

    String MAJOR = "major";
    String MANAGEMENT_MAJOR_VERSION = "management-major-version";
    String MANAGEMENT_MICRO_VERSION = "management-micro-version";
    String MANAGEMENT_MINOR_VERSION = "management-minor-version";
    String MANAGEMENT_MODEL = "management-model";
    String MAP_CLEAR = "map-clear";
    String MAP_GET = "map-get";
    String MAP_PUT = "map-put";
    String MAP_REMOVE = "map-remove";
    String MAX = "max";
    String MAX_LENGTH = "max-length";
    String MIN = "min";
    String MIN_LENGTH = "min-length";
    String MINOR = "minor";

    String NAME = "name";
    String NILLABLE = "nillable";

    String OP = "operation";
    String OPERATION_NAME = "operation-name";
    String OPERATIONS = "operations";
    String ORDINAL = "ordinal";
    String OUTCOME = "outcome";

    String PARENT = "parent";
    String PATCH = "patch";
    String PRODUCT_NAME = "product-name";
    String PRODUCT_VERSION = "product-version";

    String QUERY = "query";

    String READ_ATTRIBUTE = "read-attribute";
    String READ_ATTRIBUTE_GROUP = "read-attribute-group";
    String READ_ATTRIBUTE_GROUP_NAMES = "read-attribute-group-names";
    String READ_CHILDREN_NAMES = "read-children-names";
    String READ_CHILDREN_RESOURCES = "read-children-resources";
    String READ_CHILDREN_TYPES = "read-children-types";
    String READ_ONLY = "read-only";
    String READ_OPERATION_DESCRIPTION = "read-operation-description";
    String READ_OPERATION_NAMES = "read-operation-names";
    String READ_RESOURCE_DESCRIPTION = "read-resource-description";
    String READ_RESOURCE = "read-resource";
    String REASON = "reason";
    String RELEASE_CODENAME = "release-codename";
    String RELEASE_VERSION = "release-version";
    String REMOVE = "remove";
    String REPLY_PROPERTIES = "reply-properties";
    String REQUEST_PROPERTIES = "request-properties";
    String REQUIRED = "required";
    String REQUIRES = "requires";
    String RESTART_REQUIRED = "restart-required";
    String RESULT = "result";
    String RETURN_VALUE = "return-value";
    String RUNTIME_ONLY = "runtime-only";

    String SENSITIVE = "sensitive";
    String SINCE = "since";
    String SINGLETON = "singleton";
    String STORAGE = "storage";
    String SUCCESS = "success";

    String TYPE = "type";

    String UNDEFINE_ATTRIBUTE = "undefine-attribute";
    String UNIT = "unit";

    String VALUE_TYPE = "value-type";

    String WHOAMI = "whoami";
    String WRITE_ATTRIBUTE = "write-attribute";
}

