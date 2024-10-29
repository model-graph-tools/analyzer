#!/usr/bin/env kotlin

@file:DependsOn("com.github.ajalt.clikt:clikt-jvm:4.3.0")
@file:DependsOn("com.lordcodes.turtle:turtle:0.9.0")
@file:DependsOn("io.github.z4kn4fein:semver-jvm:2.0.0")

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.mordant.rendering.TextColors.*
import io.github.z4kn4fein.semver.Version
import kotlin.system.exitProcess

val neo4jVersion = "5.24.2"

data class WildFly(val version: Version, val repository: String, val jdk: String? = null) {
    val majorMinor: String
        get() = if (version.minor != 0) "${version.major}.${version.minor}" else version.major.toString()

    val image: String = "${repository}:${version}.Final${jdk?.let { "-$it" } ?: ""}"

    val httpPort: Int
        get() = if (version.minor != 0) "8${version.major}${version.minor}".toInt() else "80${version.major}".toInt()

    val managementPort: Int
        get() = if (version.minor != 0) "9${version.major}${version.minor}".toInt() else "99${version.major}".toInt()
}

// only one minor per major is allowed!
val wildFlyVersions: Map<Version, WildFly> = mapOf(
    Version(10, 0, 0) to WildFly(Version(10, 0, 0), "jboss/wildfly"),
    Version(10, 1, 0) to WildFly(Version(10, 1, 0), "jboss/wildfly"),
    Version(11, 0, 0) to WildFly(Version(11, 0, 0), "jboss/wildfly"),
    Version(12, 0, 0) to WildFly(Version(12, 0, 0), "jboss/wildfly"),
    Version(13, 0, 0) to WildFly(Version(13, 0, 0), "jboss/wildfly"),
    Version(14, 0, 1) to WildFly(Version(14, 0, 1), "jboss/wildfly"),
    Version(15, 0, 1) to WildFly(Version(15, 0, 1), "jboss/wildfly"),
    Version(16, 0, 0) to WildFly(Version(16, 0, 0), "jboss/wildfly"),
    Version(17, 0, 1) to WildFly(Version(17, 0, 1), "jboss/wildfly"),
    Version(18, 0, 1) to WildFly(Version(18, 0, 1), "jboss/wildfly"),
    Version(19, 0, 0) to WildFly(Version(19, 0, 0), "jboss/wildfly"),
    Version(19, 1, 0) to WildFly(Version(19, 1, 0), "jboss/wildfly"),
    Version(20, 0, 1) to WildFly(Version(20, 0, 1), "jboss/wildfly"),
    Version(21, 0, 2) to WildFly(Version(21, 0, 2), "jboss/wildfly"),
    Version(22, 0, 1) to WildFly(Version(22, 0, 1), "jboss/wildfly"),
    Version(23, 0, 2) to WildFly(Version(23, 0, 2), "quay.io/wildfly"),
    Version(24, 0, 0) to WildFly(Version(24, 0, 0), "quay.io/wildfly"),
    Version(25, 0, 1) to WildFly(Version(25, 0, 1), "quay.io/wildfly"),
    Version(26, 0, 1) to WildFly(Version(26, 0, 1), "quay.io/wildfly"),
    Version(26, 1, 3) to WildFly(Version(26, 1, 3), "quay.io/wildfly", "jdk17"),
    Version(27, 0, 1) to WildFly(Version(27, 0, 1), "quay.io/wildfly", "jdk19"),
    Version(28, 0, 1) to WildFly(Version(28, 0, 1), "quay.io/wildfly", "jdk20"),
    Version(29, 0, 1) to WildFly(Version(29, 0, 1), "quay.io/wildfly", "jdk20"),
    Version(30, 0, 1) to WildFly(Version(30, 0, 1), "quay.io/wildfly", "jdk20"),
    Version(31, 0, 1) to WildFly(Version(31, 0, 1), "quay.io/wildfly", "jdk20"),
    Version(32, 0, 1) to WildFly(Version(32, 0, 1), "quay.io/wildfly", "jdk21"),
    Version(33, 0, 2) to WildFly(Version(33, 0, 2), "quay.io/wildfly", "jdk21"),
    Version(34, 0, 0) to WildFly(Version(34, 0, 0), "quay.io/wildfly", "jdk21"),
)

fun CliktCommand.step(message: String, ignoreError: Boolean = false, code: () -> Unit) {
    echo("${yellow("…")} $message", trailingNewline = false)
    try {
        code.invoke()
    } catch (e: Exception) {
        if (!ignoreError) {
            echo("\r${red("x")}\n\n${red("Error: ")} ${e.message}", err = true)
        }
    }
    echo("\r${green("✓")}")
}

fun CliktCommand.fail(message: String) {
    echo("\r${red("x")}\n\n${red("Error: ")} $message", err = true)
    exitProcess(1)
}
