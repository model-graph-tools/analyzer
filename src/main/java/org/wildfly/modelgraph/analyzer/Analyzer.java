package org.wildfly.modelgraph.analyzer;

import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.multimap.MutableMultimap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.multimap.list.FastListMultimap;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.modelgraph.analyzer.dmr.Operation;
import org.wildfly.modelgraph.analyzer.dmr.ResourceAddress;
import org.wildfly.modelgraph.analyzer.dmr.WildFlyClient;
import org.wildfly.modelgraph.analyzer.neo4j.Cypher;
import org.wildfly.modelgraph.analyzer.neo4j.Neo4jClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.wildfly.modelgraph.analyzer.dmr.ModelDescriptionConstants.*;

class Analyzer {

    private static final int MAX_DEPTH = 10;
    private static final Logger logger = LoggerFactory.getLogger(Analyzer.class);
    private static final ImmutableSet<String> GLOBAL_OPERATIONS = Sets.immutable.of(
            // ADD is not stored as a global operation (each ADD operation is different)
            LIST_ADD,
            LIST_CLEAR,
            LIST_GET,
            LIST_REMOVE,
            MAP_CLEAR,
            MAP_GET,
            MAP_PUT,
            MAP_REMOVE,
            QUERY,
            READ_ATTRIBUTE,
            READ_ATTRIBUTE_GROUP,
            READ_ATTRIBUTE_GROUP_NAMES,
            READ_CHILDREN_NAMES,
            READ_CHILDREN_RESOURCES,
            READ_CHILDREN_TYPES,
            READ_OPERATION_DESCRIPTION,
            READ_OPERATION_NAMES,
            READ_RESOURCE_DESCRIPTION,
            READ_RESOURCE,
            REMOVE,
            UNDEFINE_ATTRIBUTE,
            WHOAMI,
            WRITE_ATTRIBUTE);

    private final WildFlyClient wc;
    private final Neo4jClient nc;
    private final Stats stats;
    private final Set<String> missingGlobalOperations;

    Analyzer(WildFlyClient wc, Neo4jClient nc) {
        this.wc = wc;
        this.nc = nc;
        this.stats = new Stats();
        this.missingGlobalOperations = Sets.mutable.ofAll(GLOBAL_OPERATIONS.castToSet());
    }

    void start(String resource) {
        stats.start();
        identity();
        parse(ResourceAddress.of(resource), null);
        stats.stop();
    }

    // ------------------------------------------------------ management model

    private void identity() {
        var operation = new Operation.Builder(READ_RESOURCE, ResourceAddress.of("/"))
                .param(ATTRIBUTES_ONLY, true)
                .param(INCLUDE_RUNTIME, true)
                .build();
        var modelNode = wc.execute(operation);
        writeIdentity(modelNode);
    }

    private void parse(ResourceAddress address, ResourceAddress parent) {
        if (address.size() < MAX_DEPTH) {
            parseResource(address, parent);
            for (var child : readChildren(address)) {
                parse(address.add(child), address);
            }
        } else {
            logger.warn("Skip {}. Maximum nesting of {} reached.", address, MAX_DEPTH);
        }
    }

    private void parseResource(ResourceAddress address, ResourceAddress parent) {
        var rrd = new Operation.Builder(READ_RESOURCE_DESCRIPTION, address)
                .param(INCLUDE_ALIASES, true)
                .param(OPERATIONS, true)
                .build();

        var resourceDescription = wc.execute(rrd);
        if (resourceDescription.isDefined()) {
            logger.info("Read {}", address.toString());

            // for a foo=* address, the result is an array
            if (resourceDescription.getType() == ModelType.LIST) {
                var descriptions = resourceDescription.asList();
                if (!descriptions.isEmpty() && descriptions.get(0).hasDefined(RESULT)) {
                    resourceDescription = descriptions.get(0).get(RESULT);
                }
            }

            createResource(address, resourceDescription);
            if (parent != null) {
                mergeChildOf(address, parent);
            }

            // capabilities
            if (resourceDescription.hasDefined(CAPABILITIES)) {
                for (var capability : resourceDescription.get(CAPABILITIES).asList()) {
                    mergeDeclaresCapabilities(address, capability);
                }
            }

            // attributes
            if (resourceDescription.hasDefined(ATTRIBUTES)) {
                mergeAttributes(address, new ArrayList<>(), resourceDescription.get(ATTRIBUTES).asPropertyList());
            }

            // operations
            if (resourceDescription.hasDefined(OPERATIONS)) {
                for (var property : resourceDescription.get(OPERATIONS).asPropertyList()) {
                    var name = property.getName();
                    var operation = property.getValue();
                    var globalOperation = GLOBAL_OPERATIONS.contains(name);
                    var create = !globalOperation || missingGlobalOperations.contains(name);

                    if (create) {
                        mergeOperation(address, name, operation, globalOperation);
                        if (operation.hasDefined(REQUEST_PROPERTIES)) {
                            mergeParameters(address, name, new ArrayList<>(),
                                    operation.get(REQUEST_PROPERTIES).asPropertyList());
                        }
                        if (globalOperation) {
                            missingGlobalOperations.remove(name);
                        }
                    } else {
                        linkGlobalOperation(address, name);
                    }
                }
            }
        } else {
            stats.failedResources++;
        }
    }

    private List<String> readChildren(ResourceAddress address) {
        var rct = new Operation.Builder(READ_CHILDREN_TYPES, address)
                .param(INCLUDE_SINGLETONS, true)
                .build();

        var result = wc.execute(rct);
        if (result.isDefined()) {
            return result.asList().stream().map(ModelNode::asString).collect(toList());
        }
        return emptyList();
    }

    // ------------------------------------------------------ resources

    private void writeIdentity(ModelNode identityNode) {
        String productName = identityNode.get(PRODUCT_NAME).asString("WildFly");
        String productVersion = identityNode.get(PRODUCT_VERSION).asString("0.0.0");
        int major = identityNode.get(MANAGEMENT_MAJOR_VERSION).asInt();
        int minor = identityNode.get(MANAGEMENT_MINOR_VERSION).asInt();
        int patch = identityNode.get(MANAGEMENT_MICRO_VERSION).asInt();
        String managementVersion = String.format("%d.%d.%d", major, minor, patch);
        String identifier = String.format("%s-%s-mgt-%s",
                Strings.identify(productName), productVersion, managementVersion);

        var cypher = new Cypher("CREATE (:Identity {")
                .append(IDENTIFIER, identifier).comma()
                .append(PRODUCT_NAME, productName).comma()
                .append(PRODUCT_VERSION, productVersion).comma()
                .append(MANAGEMENT_VERSION, managementVersion)
                .append("})");

        nc.execute(cypher);
        stats.resources++;
    }

    private void createResource(ResourceAddress address, ModelNode modelNode) {
        var cypher = new Cypher("CREATE (r:Resource {")
                .append(NAME, address.getName()).comma()
                .append(ADDRESS, address.toString()).comma()
                .append(SINGLETON, address.isSingleton());
        appendIfPresent(cypher, DESCRIPTION, modelNode, ModelNode::asString);
        if (modelNode.hasDefined(CHILDREN)) {
            // Ugly workaround to save child descriptions. Only reason is to save descriptions of
            // none existing 'parent-singleton-resources' such as "/core-service=management/access"
            // Such resources actually don't exist on its own. Only the singleton child resources like
            //    "access=audit"
            //    "access=authorization"
            //    "access=identity"
            // exist.
            // But nevertheless the parent resource holds also descriptions for such resources.
            var childDescriptions = modelNode.get(CHILDREN).asPropertyList().stream()
                    .map(property -> {
                        String childDescription = property.getValue().get(DESCRIPTION)
                                .asString("No description available for " + property.getName());
                        return property.getName() + "|" + childDescription;
                    })
                    .collect(joining("^"));
            cypher.comma().append(CHILD_DESCRIPTIONS, childDescriptions);
        }
        cypher.append("})"); // end resource
        mergeDeprecated(cypher, "r", modelNode, address.toString());

        var counters = nc.execute(cypher);
        stats.resources += counters.nodesCreated();
    }

    private void mergeChildOf(ResourceAddress child, ResourceAddress parent) {
        var cypher = new Cypher("MATCH (child:Resource {")
                .append(ADDRESS, CHILD, child.toString()).append("}),")
                .append("(parent:Resource {")
                .append(ADDRESS, PARENT, parent.toString()).append("})")
                .append(" MERGE (child)-[:CHILD_OF]->(parent)");

        var counters = nc.execute(cypher);
        stats.relations += counters.relationshipsCreated();
    }

    private Cypher matchResource(ResourceAddress address) {
        return new Cypher("MATCH (r:Resource {").append(ADDRESS, address.toString()).append("})");
    }

    // ------------------------------------------------------ capabilities

    private void mergeDeclaresCapabilities(ResourceAddress address, ModelNode capability) {
        var cypher = matchResource(address)
                .append(" MERGE (c:Capability {")
                .append(NAME, capability.get(NAME).asString())
                .append("}) MERGE (r)-[:DECLARES_CAPABILITY]->(c)");

        var counters = nc.execute(cypher);
        stats.capabilities += counters.nodesCreated();
        stats.relations += counters.relationshipsCreated();
    }

    // ------------------------------------------------------ attributes

    private void mergeAttributes(ResourceAddress address, List<String> path, List<Property> properties) {
        MutableMultimap<String, String> alternatives = new FastListMultimap<>();
        MutableMultimap<String, String> requires = new FastListMultimap<>();

        for (var property : properties) {
            var name = property.getName();
            var attribute = property.getValue();
            mergeAttribute(address, path, name, attribute);

            // complex attributes
            if (attribute.hasDefined(VALUE_TYPE)) {
                var valueType = attribute.get(VALUE_TYPE);
                if (valueType.getType() == ModelType.OBJECT) {
                    List<String> newPath = new ArrayList<>(path);
                    newPath.add(name);
                    mergeAttributes(address, newPath, valueType.asPropertyList());
                }
            }

            // references capability
            if (attribute.hasDefined(CAPABILITY_REFERENCE)) {
                var capabilityReference = attribute.get(CAPABILITY_REFERENCE).asString();
                mergeAttributeReferencesCapability(address, path, name, capabilityReference);
            }

            // sensitivity
            if (attribute.hasDefined(ACCESS_CONSTRAINTS) &&
                    attribute.get(ACCESS_CONSTRAINTS).hasDefined(SENSITIVE)) {
                var sensitive = attribute.get(ACCESS_CONSTRAINTS).get(SENSITIVE);
                if (sensitive.isDefined()) {
                    mergeSensitive(address, path, name, sensitive);
                }
            }

            // collect alternatives and requires
            if (attribute.hasDefined(ALTERNATIVES)) {
                var a = attribute.get(ALTERNATIVES)
                        .asList()
                        .stream()
                        .map(ModelNode::asString)
                        .collect(toList());
                alternatives.putAll(name, a);
            }
            if (attribute.hasDefined(REQUIRES)) {
                var r = attribute.get(REQUIRES)
                        .asList()
                        .stream()
                        .map(ModelNode::asString)
                        .collect(toList());
                requires.putAll(name, r);
            }
        }

        // post process alternatives and requires
        alternatives.forEachKeyValue((key, value) ->
                mergeAttributeRelation(address, path, key, value, "-[:ALTERNATIVE]-"));
        requires.forEachKeyValue((key, value) ->
                mergeAttributeRelation(address, path, key, value, "-[:REQUIRES]->"));
    }

    private void mergeAttribute(ResourceAddress address, List<String> path, String name, ModelNode attribute) {
        var cypher = matchResource(address);
        if (path.isEmpty()) {
            cypher.append(" MERGE (r)-[:HAS_ATTRIBUTE]->");
        } else {
            appendPath(cypher, path, "Attribute", "HAS_ATTRIBUTE",
                    (c, v) -> c.append(String.format(" MERGE (%s)-[:CONSISTS_OF]->", v)));
        }
        cypher.append("(a:Attribute {").append(NAME, name);
        appendCommonProperties(cypher, attribute);
        appendIfPresent(cypher, ACCESS_TYPE, attribute, ModelNode::asString);
        appendIfPresent(cypher, ALIAS, attribute, ModelNode::asString);
        appendIfPresent(cypher, ATTRIBUTE_GROUP, attribute, ModelNode::asString);
        appendIfPresent(cypher, DEFAULT, attribute, ModelNode::asString);
        appendIfPresent(cypher, DESCRIPTION, attribute, ModelNode::asString);
        appendIfPresent(cypher, RESTART_REQUIRED, attribute, ModelNode::asString);
        appendIfPresent(cypher, STORAGE, attribute, ModelNode::asString);
        cypher.append("})"); // end attribute
        mergeDeprecated(cypher, "a", attribute, String.format("%s@%s",
                address, (path.isEmpty() ? name : (String.join(".", path) + "." + name))));

        var counters = nc.execute(cypher);
        stats.attributes += counters.nodesCreated();
        stats.relations += counters.relationshipsCreated();
    }

    private void mergeAttributeReferencesCapability(ResourceAddress address, List<String> path, String name,
                                                    String capability) {
        var cypher = matchResource(address);
        if (path.isEmpty()) {
            cypher.append("-[:HAS_ATTRIBUTE]->");
        } else {
            appendPath(cypher, path, "Attribute", "HAS_ATTRIBUTE");
            cypher.append("-[:CONSISTS_OF]->");
        }
        cypher.append("(a:Attribute {").append(NAME, name).append("})")
                .append(" MATCH (c:Capability {")
                .append(NAME, CAPABILITY_REFERENCE, capability)
                .append("}) MERGE (a)-[:REFERENCES_CAPABILITY]->(c)");

        var counters = nc.execute(cypher);
        stats.relations += counters.relationshipsCreated();
    }

    private void mergeSensitive(ResourceAddress address, List<String> path, String name, ModelNode sensitive) {
        for (var property : sensitive.asPropertyList()) {
            var sensitiveName = property.getName();
            var type = property.getValue().get(TYPE).asString();

            var cypher = matchResource(address);
            if (path.isEmpty()) {
                cypher.append("-[:HAS_ATTRIBUTE]->");
            } else {
                appendPath(cypher, path, "Attribute", "HAS_ATTRIBUTE");
                cypher.append("-[:CONSISTS_OF]->");
            }
            cypher.append("(a:Attribute {").append(NAME, name).append("})");
            cypher.append(" MERGE (a)-[:IS_SENSITIVE]->(:Constraint {")
                    .append(NAME, "sensitiveName", sensitiveName).comma()
                    .append(TYPE, type).append("})");

            var counters = nc.execute(cypher);
            stats.sensitive += counters.nodesCreated();
            stats.relations += counters.relationshipsCreated();
        }
    }

    private void mergeAttributeRelation(ResourceAddress address, List<String> path,
                                        String source, String target, String relation) {
        var cypher = matchResource(address);
        if (path.isEmpty()) {
            cypher.append("-[:HAS_ATTRIBUTE]->(source:Attribute {")
                    .append(NAME, "sourceName", source).append("})")
                    .append(" MATCH (r)-[:HAS_ATTRIBUTE]->(target:Attribute {")
                    .append(NAME, "targetName", target).append("})");
        } else {
            appendPath(cypher, path, "Attribute", "HAS_ATTRIBUTE");
            cypher.append("-[:CONSISTS_OF]->(source:Attribute {")
                    .append(NAME, "sourceName", source).append("})")
                    .append(" MATCH (r)");
            appendPath(cypher, path, "Attribute", "HAS_ATTRIBUTE");
            cypher.append("-[:CONSISTS_OF]->(target:Attribute {")
                    .append(NAME, "targetName", target).append("})");
        }
        cypher.append(" MERGE (source)").append(relation).append("(target)");

        var counters = nc.execute(cypher);
        stats.relations += counters.relationshipsCreated();
    }

    // ------------------------------------------------------ operations

    private void mergeOperation(ResourceAddress address, String name, ModelNode operation, boolean globalOperation) {
        var cypher = matchResource(address)
                .append(" MERGE (r)-[:PROVIDES]->(o:Operation {")
                .append(NAME, name).comma()
                .append(GLOBAL, globalOperation);
        appendIfPresent(cypher, DESCRIPTION, operation, ModelNode::asString);
        appendIfPresent(cypher, READ_ONLY, operation, ModelNode::asBoolean);
        appendIfPresent(cypher, RUNTIME_ONLY, operation, ModelNode::asBoolean);
        if (operation.hasDefined(REPLY_PROPERTIES)) {
            var replyNode = operation.get(REPLY_PROPERTIES);
            if (replyNode.isDefined()) {
                appendIfPresent(cypher, RETURN_VALUE, replyNode, TYPE, (value -> value.asType().name()));
                appendValueType(cypher, replyNode);
            }
        }
        cypher.append("})"); // end operation
        mergeDeprecated(cypher, "o", operation, String.format("%s:%s", address, name));

        var counters = nc.execute(cypher);
        stats.operations += counters.nodesCreated();
        stats.relations += counters.relationshipsCreated();
    }

    private void linkGlobalOperation(ResourceAddress address, String name) {
        var cypher = matchResource(address).comma()
                .append("(o:Operation{")
                .append(NAME, name).append("})")
                .append(" MERGE (r)-[:PROVIDES]->(o)");

        var counters = nc.execute(cypher);
        stats.relations += counters.relationshipsCreated();
    }

    private Cypher matchOperation(ResourceAddress address, String operation) {
        return matchResource(address)
                .append("-[:PROVIDES]->(o:Operation {").append(NAME, OPERATION_NAME, operation).append("})");
    }

    // ------------------------------------------------------ parameters

    private void mergeParameters(ResourceAddress address, String operation, List<String> path,
                                 List<Property> properties) {
        MutableMultimap<String, String> alternatives = new FastListMultimap<>();
        MutableMultimap<String, String> requires = new FastListMultimap<>();
        for (var property : properties) {
            var name = property.getName();
            var parameter = property.getValue();
            mergeParameter(address, operation, path, name, parameter);

            // complex attributes
            if (parameter.hasDefined(VALUE_TYPE)) {
                var valueType = parameter.get(VALUE_TYPE);
                if (valueType.getType() == ModelType.OBJECT) {
                    List<String> newPath = new ArrayList<>(path);
                    newPath.add(name);
                    mergeParameters(address, operation, newPath, valueType.asPropertyList());
                }
            }

            // references capability
            if (parameter.hasDefined(CAPABILITY_REFERENCE)) {
                var capabilityReference = parameter.get(CAPABILITY_REFERENCE).asString();
                mergeParameterReferencesCapability(address, operation, path, name, capabilityReference);
            }

            // collect alternatives and requires
            if (parameter.hasDefined(ALTERNATIVES)) {
                var a = parameter.get(ALTERNATIVES)
                        .asList()
                        .stream()
                        .map(ModelNode::asString)
                        .collect(toList());
                alternatives.putAll(name, a);
            }
            if (parameter.hasDefined(REQUIRES)) {
                var r = parameter.get(REQUIRES)
                        .asList()
                        .stream()
                        .map(ModelNode::asString)
                        .collect(toList());
                requires.putAll(name, r);
            }
        }

        // post process alternatives and requires
        alternatives.forEachKeyValue((key, value) ->
                mergeParameterRelation(address, operation, path, key, value, "-[:ALTERNATIVE]-"));
        requires.forEachKeyValue((key, value) ->
                mergeParameterRelation(address, operation, path, key, value, "-[:REQUIRES]->"));
    }

    private void mergeParameter(ResourceAddress address, String operation, List<String> path,
                                String name, ModelNode parameter) {
        var cypher = matchOperation(address, operation);
        if (path.isEmpty()) {
            cypher.append(" MERGE (o)-[:ACCEPTS]->");
        } else {
            appendPath(cypher, path, "Parameter", "ACCEPTS",
                    (c, v) -> c.append(String.format(" MERGE (%s)-[:CONSISTS_OF]->", v)));
        }
        cypher.append("(p:Parameter {").append(NAME, name);
        appendCommonProperties(cypher, parameter);
        cypher.append("})"); // end parameter
        mergeDeprecated(cypher, "p", parameter, String.format("%s:%s(%s)",
                address.toString(), operation, (path.isEmpty() ? name : (String.join(".", path) + "." + name))));

        var counters = nc.execute(cypher);
        stats.parameters += counters.nodesCreated();
        stats.relations += counters.relationshipsCreated();
    }

    private void mergeParameterReferencesCapability(ResourceAddress address, String operation, List<String> path,
                                                    String name, String capability) {
        var cypher = matchOperation(address, operation);
        if (path.isEmpty()) {
            cypher.append("-[:ACCEPTS]->");
        } else {
            appendPath(cypher, path, "Parameter", "ACCEPTS");
            cypher.append("-[:CONSISTS_OF]->");
        }
        cypher.append("(p:Parameter {").append(NAME, name).append("})")
                .append(" MATCH (c:Capability {")
                .append(NAME, CAPABILITY_REFERENCE, capability)
                .append("}) MERGE (p)-[:REFERENCES_CAPABILITY]->(c)");

        var counters = nc.execute(cypher);
        stats.relations += counters.relationshipsCreated();
    }

    private void mergeParameterRelation(ResourceAddress address, String operation, List<String> path,
                                        String source, String target, String relation) {
        var cypher = matchOperation(address, operation);
        if (path.isEmpty()) {
            cypher.append("-[:ACCEPTS]->(source:Parameter {")
                    .append(NAME, "sourceName", source).append("})")
                    .append(" MATCH (o)-[:ACCEPTS]->(target:Parameter {")
                    .append(NAME, "targetName", target).append("})");
        } else {
            appendPath(cypher, path, "Parameter", "ACCEPTS");
            cypher.append("-[:CONSISTS_OF]->(source:Parameter {")
                    .append(NAME, "sourceName", source).append("})")
                    .append(" MATCH (r)-[:PROVIDES]->(o)");
            appendPath(cypher, path, "Parameter", "ACCEPTS");
            cypher.append("-[:CONSISTS_OF]->(target:Parameter {")
                    .append(NAME, "targetName", target).append("})");
        }
        cypher.append(" MERGE (source)").append(relation).append("(target)");

        var counters = nc.execute(cypher);
        stats.relations += counters.relationshipsCreated();
    }

    // ------------------------------------------------------ helper methods

    private void appendPath(Cypher cypher, List<String> path, String type, String relation) {
        appendPath(cypher, path, type, relation, null);
    }

    private void appendPath(Cypher cypher, List<String> path, String type, String relation,
                            BiConsumer<Cypher, String> variableConsumer) {
        var i = 0;
        String variable = null;
        for (var iterator = path.listIterator(); iterator.hasNext(); i++) {
            variable = String.format("var%d", i);
            cypher.append(String.format("-[:%s]->", (iterator.hasPrevious() ? "CONSISTS_OF" : relation)))
                    .append(String.format("(%s:%s {", variable, type))
                    .append(NAME, String.format("name%d", i), iterator.next())
                    .append("})");
        }
        if (variableConsumer != null) {
            variableConsumer.accept(cypher, variable);
        }
    }

    private void appendCommonProperties(Cypher cypher, ModelNode modelNode) {
        appendIfPresent(cypher, EXPRESSIONS_ALLOWED, modelNode, ModelNode::asBoolean);
        appendIfPresent(cypher, MAX, modelNode, ModelNode::asLong);
        appendIfPresent(cypher, MAX_LENGTH, modelNode, ModelNode::asLong);
        appendIfPresent(cypher, MIN, modelNode, ModelNode::asLong);
        appendIfPresent(cypher, MIN_LENGTH, modelNode, ModelNode::asLong);
        appendIfPresent(cypher, NILLABLE, modelNode, ModelNode::asBoolean);
        appendIfPresent(cypher, REQUIRED, modelNode, ModelNode::asBoolean);
        appendIfPresent(cypher, TYPE, modelNode, ModelNode::asString);
        appendIfPresent(cypher, UNIT, modelNode, ModelNode::asString);
        appendValueType(cypher, modelNode);
    }

    private void appendValueType(Cypher cypher, ModelNode modelNode) {
        if (modelNode.hasDefined(VALUE_TYPE)) {
            var valueTypeNode = modelNode.get(VALUE_TYPE);
            if (ModelType.STRING == valueTypeNode.getType()) {
                cypher.comma().append(VALUE_TYPE, valueTypeNode.asString());
            } else {
                cypher.comma().append(VALUE_TYPE, ModelType.OBJECT.name());
            }
        }
    }

    private void mergeDeprecated(Cypher cypher, String name, ModelNode modelNode, String context) {
        if (modelNode.hasDefined(DEPRECATED)) {
            var deprecatedNode = modelNode.get(DEPRECATED);
            String reason = deprecatedNode.get(REASON).asString();
            String sinceValue = deprecatedNode.get(SINCE).asString();
            int[] sinceParsed = parseVersion(sinceValue);
            if (sinceParsed != null) {
                cypher.append(" MERGE (v:Version {")
                        .append(MAJOR, sinceParsed[0]).comma()
                        .append(MINOR, sinceParsed[1]).comma()
                        .append(PATCH, sinceParsed[2]).comma()
                        .append(ORDINAL, versionOrdinal(sinceParsed[0], sinceParsed[1], sinceParsed[2]))
                        .append("}) MERGE (" + name + ")-[:DEPRECATED_SINCE {")
                        .append(REASON, reason)
                        .append("}]->(v)");
            } else {
                stats.errors.add(String.format("Unable to parse deprecation version '%s' for '%s'",
                        sinceValue, context));
            }
        }
    }

    private int[] parseVersion(String value) {
        int[] version = new int[3];
        if (value != null && value.length() != 0) {
            try {
                String[] parts = value.split("\\.");
                if (parts.length == 3) {
                    for (int i = 0; i < parts.length; i++) {
                        version[i] = Integer.parseInt(parts[i]);
                    }
                    return version;
                } else {
                    return null;
                }
            } catch (NumberFormatException e) {
                return null;
            }
        } else {
            return null;
        }
    }

    private int versionOrdinal(int major, int minor, int patch) {
        int ordinal = 0;
        int[] numbers = new int[]{patch, minor, major};
        for (int i = 0; i < numbers.length; i++) {
            ordinal |= numbers[i] << i * 10;
        }
        return ordinal;
    }

    private <T> void appendIfPresent(Cypher cypher, String name, ModelNode modelNode, Function<ModelNode, T> getValue) {
        appendIfPresent(cypher, name, modelNode, name, getValue);
    }

    private <T> void appendIfPresent(Cypher cypher, String name, ModelNode modelNode, String attribute,
                                     Function<ModelNode, T> getValue) {
        if (modelNode.hasDefined(attribute)) {
            var value = modelNode.get(attribute);
            // must not be the first append(name, value) call!
            cypher.comma().append(name, getValue.apply(value));
        }
    }

    // ------------------------------------------------------ properties

    Stats stats() {
        return stats;
    }
}
