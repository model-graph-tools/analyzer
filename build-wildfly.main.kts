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

class BuildWildFlyCommand : CliktCommand(name = "build-wildfly") {

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
        step("Building image for ${cyan("WildFly ${wildFly.version})} from ${cyan(wildFly.image)}") {
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
}

BuildWildFlyCommand().main(args)
