#!/usr/bin/env kotlin

@file:DependsOn("com.github.ajalt.clikt:clikt-jvm:4.3.0")
@file:DependsOn("com.lordcodes.turtle:turtle:0.9.0")
@file:DependsOn("io.github.z4kn4fein:semver-jvm:2.0.0")
@file:Import("script-commons.main.kts")
@file:Suppress("MemberVisibilityCanBePrivate")

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.convert
import com.github.ajalt.clikt.parameters.arguments.help
import com.github.ajalt.mordant.rendering.TextColors.cyan
import com.lordcodes.turtle.shellRun
import io.github.z4kn4fein.semver.toVersionOrNull
import kotlin.io.path.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.pathString

val scriptDependencies = listOf("start-neo4j.main.kts", "analyze.main.kts")

@OptIn(kotlin.io.path.ExperimentalPathApi::class)
class StartWildFly : CliktCommand(name = "start-wildfly") {

    val wildFly: WildFly by argument("version")
        .help("The WildFly version as <major> or <major>.<minor>")
        .convert {
            val version = it.toVersionOrNull(strict = false)
            if (version != null) {
                wildFlyVersions[version] ?: fail(
                    "$it is not a supported version.\nSupported versions are ${wildFlyVersions.values.map { it.majorMinor }}"
                )
            } else {
                fail("$it is not a valid version")
            }
        }

    override fun run() {
        val dataDirectory = Path("/tmp/mgt/data/${wildFly.majorMinor}")
        val dumpDirectory = Path("/tmp/mgt/dump/${wildFly.majorMinor}")

        step("Start ${cyan("WildFly ${wildFly.version})} from ${cyan(wildFly.image)}") {
            shellRun(
                "docker", listOf(
                    "build",
                    "--build-arg",
                    "WILDFLY_IMAGE=${wildFly.image}",
                    "--file",
                    "src/main/docker/wildfly/Dockerfile",
                    "--tag",
                    "quay.io/modelgraphtools/wildfly:${wildFly.version}",
                    "src/main/docker/wildfly"
                )
            )
        }
        }

        step("Prepare ${cyan("dump")} directory") {
            dumpDirectory.deleteRecursively()
            dumpDirectory.createDirectory()
        }

        step("Stop ${cyan("analyzer")}", ignoreError = true) {
            shellRun(
                "docker", listOf(
                    "stop",
                    "mgt-analyze-${wildFly.majorMinor}",
                    "2>/dev/null",
                )
            )
        }

        step("Start ${cyan("Neo4J $neo4jVersion")}") {
            shellRun(
                "docker", listOf(
                    "run",
                    "--interactive",
                    "--tty",
                    "--rm",
                    "--volume",
                    "${dataDirectory.pathString}:/data",
                    "--volume",
                    "${dumpDirectory.pathString}:/dump",
                    "--user",
                    "\$(id -u):\$(id -g)",
                    "neo4j:${neo4jVersion}",
                    "neo4j-admin database dump --to-path=/dump neo4j"
                )
            )
        }

        step("Copy ${cyan("entrypoint")}") {
            shellRun("cp", listOf("src/main/docker/neo4j/mgt-entrypoint.sh", dumpDirectory.pathString))
        }

        step("Build ${cyan("Neo4J $neo4jVersion")}") {
            shellRun(
                "docker", listOf(
                    "build",
                    "--build-arg",
                    "NEO4J_VERSION=$neo4jVersion",
                    "--file",
                    "src/main/docker/neo4j/Dockerfile",
                    "--tag",
                    "quay.io/modelgraphtools/neo4j:${wildFly.version}",
                    dumpDirectory.pathString
                )
            )
        }
    }
}

StartWildFlyCommand().main(args)
