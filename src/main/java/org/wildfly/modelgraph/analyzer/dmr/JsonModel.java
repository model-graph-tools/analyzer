package org.wildfly.modelgraph.analyzer.dmr;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonPointer;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.modelgraph.analyzer.Version;

public class JsonModel implements ManagementModel {

    private static final Logger logger = LoggerFactory.getLogger(JsonModel.class);
    private static final String METADATA_JSON = "doc/META-INF/metadata.json";
    public static final String MODEL_JSON = "doc/META-INF/management-api.json";

    private final String filename;
    private final ZipFile zipFile;
    private final JsonReader metadataReader;
    private final JsonReader modelReader;
    private final JsonObject metadata;
    private final JsonObject model;

    public JsonModel(String filename) {
        this.filename = filename;
        try {
            this.zipFile = new ZipFile(filename);
            var metadataEntry = zipFile.getEntry(METADATA_JSON);
            if (metadataEntry == null) {
                throw new RuntimeException(String.format("Missing %s in %s", METADATA_JSON, filename));
            }
            this.metadataReader = Json.createReader(zipFile.getInputStream(metadataEntry));
            this.metadata = metadataReader.readObject();

            var modelEntry = zipFile.getEntry(MODEL_JSON);
            if (modelEntry == null) {
                throw new RuntimeException(String.format("Missing %s in %s", MODEL_JSON, filename));
            }
            this.modelReader = Json.createReader(zipFile.getInputStream(modelEntry));
            this.model = modelReader.readObject();
            logger.info("Loaded {} and {} from {}", METADATA_JSON, MODEL_JSON, filename);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Failed to read from zip file %s: %s", filename, e.getMessage()));
        }
    }

    // Used by tests
    JsonModel(String metadata, String model) {
        this.filename = null;
        this.zipFile = null;
        this.metadataReader = Json.createReader(new StringReader(metadata));
        this.modelReader = Json.createReader(new StringReader(model));
        this.metadata = metadataReader.readObject();
        this.model = modelReader.readObject();
    }

    @Override
    public Identity identity() {
        String groupId = metadata.getString("groupId");
        String artifactId = metadata.getString("artifactId");
        Version version = Version.parse(metadata.getString("version"));
        String name = metadata.getString("name");
        String description = metadata.getString("description");
        String url = metadata.getString("url");
        String scmUrl = metadata.getString("scm-url");
        String[] licenses = metadata.getJsonArray("licenses").stream().map(JsonValue::toString).toArray(String[]::new);
        // Galleon doc ZIPs are always feature packs — even the WildFly distribution ships as one.
        // The WILDFLY identity type is reserved for live server connections via WildFlyInstance.
        return Identity.featurePack(groupId, artifactId, name, description, version, url, scmUrl, licenses);
    }

    @Override
    public List<String> children(ResourceAddress address) {
        try {
            JsonObject target;
            if (address.asPropertyList().isEmpty()) {
                target = model;
            } else {
                JsonPointer pointer = asPointer(address);
                JsonValue value = pointer.getValue(model);
                if (value == null || value.getValueType() != JsonValue.ValueType.OBJECT) {
                    return List.of();
                }
                target = value.asJsonObject();
            }

            if (!target.containsKey("children")) {
                return List.of();
            }
            JsonObject children = target.getJsonObject("children");
            if (children == null || children.isEmpty()) {
                return List.of();
            }

            List<String> result = new ArrayList<>();
            for (String childType : children.keySet()) {
                JsonObject childValue = children.getJsonObject(childType);
                if (childValue != null && childValue.containsKey("model-description")) {
                    JsonObject modelDescription = childValue.getJsonObject("model-description");
                    if (modelDescription != null) {
                        if (modelDescription.size() == 1 && modelDescription.containsKey("*")) {
                            result.add(childType);
                        } else {
                            for (String entryName : modelDescription.keySet()) {
                                result.add(childType + "=" + entryName);
                            }
                        }
                    }
                }
            }
            return result;
        } catch (Exception e) {
            logger.warn("Unable to get children for {}: {}", address, e.getMessage());
            return List.of();
        }
    }

    @Override
    public ModelNode resourceDescription(ResourceAddress address) {
        JsonPointer pointer = asPointer(address);
        JsonValue value = pointer.getValue(model);
        if (value != null) {
            return asNode(value.asJsonObject());
        }
        return new ModelNode();
    }

    @Override
    public void close() {
        logger.debug("Closing JSON model");
        if (modelReader != null) {
            modelReader.close();
        }
        if (metadataReader != null) {
            metadataReader.close();
        }
        if (zipFile != null) {
            try {
                zipFile.close();
            } catch (IOException e) {
                logger.error("Unable to close zip file {}: {}", filename, e.getMessage());
            }
        }
    }

    JsonPointer asPointer(ResourceAddress address) {
        StringBuilder path = new StringBuilder();
        List<Property> tuples = address.asPropertyList();
        for (Property tuple : tuples) {
            path.append("/children/").append(tuple.getName()).append("/model-description/").append(tuple.getValue().asString());
        }
        return Json.createPointer(path.toString());
    }

    private ModelNode asNode(JsonObject json) {
        return ModelNode.fromJSONString(json.toString());
    }
}
