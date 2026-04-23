# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
./mvnw verify                    # build + run all tests
./mvnw test                      # run tests only
./mvnw package -DskipTests       # build fat JAR without tests
./mvnw test -Dtest=CypherTest    # run a single test class
```

The project uses Maven Wrapper (`./mvnw`). Java 25 is required (`pom.xml` sets `java.version=25`). The build produces a shaded fat JAR via `maven-shade-plugin` with `Main` as the entry point.

## Architecture

This is a CLI tool (picocli) that reads the WildFly management model and writes it as a graph into Neo4j. The pipeline is: **data source → recursive tree walk → Cypher statements → Neo4j**.

### Data Sources (`ManagementModel` interface)

Two implementations behind `ManagementModel`:

- **`WildFlyInstance`** — connects to a live WildFly server via `ModelControllerClient` (native management protocol on port 9990)
- **`JsonModel`** — reads from a documentation ZIP file containing `doc/META-INF/metadata.json` and `doc/META-INF/model.json`

Both provide: `identity()`, `children(address)`, `resourceDescription(address)`.

### Core Flow (`Analyzer`)

`Analyzer` recursively walks the management model tree (max depth 10). For each resource address it:

1. Creates a `Resource` node in Neo4j
2. Links it to its parent via `CHILD_OF`
3. Extracts and merges `Capability`, `Attribute`, `Operation`, and `Parameter` nodes
4. Handles complex/nested attributes and parameters via `CONSISTS_OF` relationships
5. Tracks `ALTERNATIVE`/`REQUIRES` relationships between attributes and parameters
6. Links deprecated elements to `Version` nodes via `DEPRECATED_SINCE`

Global operations (e.g., `read-resource`, `read-attribute`) are created once and linked to all resources rather than duplicated.

### Key Domain Types

- **`ResourceAddress`** — extends `ModelNode`, represents a DMR path like `/subsystem=undertow/server=default-server`. Its `toString()` format is used as the unique key in Neo4j.
- **`Cypher`** — query builder with parameter binding. Handles DMR attribute names containing hyphens by quoting them.
- **`Identity`** — record distinguishing WildFly instances (`wf`) from feature packs (`fp`).
- **`Stats`** — mutable counters tracking created nodes/relationships, printed at the end.

### Neo4j Graph Schema

Nodes: `Identity`, `Resource`, `Attribute`, `Capability`, `Operation`, `Parameter`, `Version`, `Constraint`

Key relationships: `CHILD_OF`, `HAS_ATTRIBUTE`, `PROVIDES` (operations), `ACCEPTS` (parameters), `DECLARES_CAPABILITY`, `REFERENCES_CAPABILITY`, `DEPRECATED_SINCE`, `CONSISTS_OF`, `ALTERNATIVE`, `REQUIRES`, `IS_SENSITIVE`

Indexes are created on `name` for Resource, Attribute, Capability, Operation, and Parameter. A uniqueness constraint exists on `Resource.address`.

## Conventions

- Uses `org.jboss.dmr.ModelNode` / `Property` throughout for management model data — these are JBoss DMR types, not standard Java
- DMR constant names live in `ModelDescriptionConstants` and are statically imported everywhere
- The `--append` flag skips existing resources (checked via `Neo4jClient.exists()`)
- Logging uses SLF4J/Logback; `--verbose` sets all loggers to DEBUG