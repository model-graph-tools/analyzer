package org.wildfly.modelgraph.analyzer;

import java.io.Serializable;
import java.util.Objects;

import static org.wildfly.modelgraph.analyzer.Preconditions.*;

/**
 * An immutable representation of a host and port.
 *
 * <p>Example usage:
 *
 * <pre>
 * HostAndPort hp = HostAndPort.fromString("[2001:db8::1]")
 *     .withDefaultPort(80)
 *     .requireBracketsForIPv6();
 * hp.getHost();   // returns "2001:db8::1"
 * hp.getPort();   // returns 80
 * hp.toString();  // returns "[2001:db8::1]:80"
 * </pre>
 *
 * <p>Here are some examples of recognized formats:
 *
 * <ul>
 * <li>example.com
 * <li>example.com:80
 * <li>192.0.2.1
 * <li>192.0.2.1:80
 * <li>[2001:db8::1]
 * <li>[2001:db8::1]:80\
 * </ul>
 *
 * <p>Note that this is not an exhaustive list, because these methods are only concerned with
 * brackets, colons, and port numbers. Full validation of the host field (if desired) is the
 * caller's responsibility.
 *
 * @author Paul Marks
 * @since 10.0
 */
public final class HostAndPort implements Serializable {

    /**
     * Magic value indicating the absence of a port number.
     */
    private static final int NO_PORT = -1;

    /**
     * Build a HostAndPort instance from separate host and port values.
     *
     * <p>Note: Non-bracketed IPv6 literals are allowed.
     *
     * @param host the host string to parse. Must not contain a port number.
     * @param port a port number from [0..65535]
     * @return if parsing was successful, a populated HostAndPort object.
     * @throws IllegalArgumentException if {@code host} contains a port number, or {@code port} is out of range.
     */
    public static HostAndPort fromParts(String host, int port) {
        checkArgument(isValidPort(port), "Port out of range: %s", port);
        var parsedHost = fromString(host);
        checkArgument(!parsedHost.hasPort(), "Host has a port: %s", host);
        return new HostAndPort(parsedHost.host, port, parsedHost.hasBracketlessColons);
    }

    /**
     * Split a freeform string into a host and port, without strict validation.
     *
     * <p>Note that the host-only formats will leave the port field undefined.
     *
     * @param hostPortString the input string to parse.
     * @return if parsing was successful, a populated HostAndPort object.
     * @throws IllegalArgumentException if nothing meaningful could be parsed.
     */
    public static HostAndPort fromString(String hostPortString) {
        checkNotNull(hostPortString);

        String host;
        String portString = null;
        var hasBracketlessColons = false;

        if (hostPortString.startsWith("[")) {
            var hostAndPort = getHostAndPortFromBracketedHost(hostPortString);
            host = hostAndPort[0];
            portString = hostAndPort[1];
        } else {
            var colonPos = hostPortString.indexOf(':');
            if (colonPos >= 0 && hostPortString.indexOf(':', colonPos + 1) == -1) {
                // Exactly 1 colon. Split into host:port.
                host = hostPortString.substring(0, colonPos);
                portString = hostPortString.substring(colonPos + 1);
            } else {
                // 0 or 2+ colons. Bare hostname or IPv6 literal.
                host = hostPortString;
                hasBracketlessColons = (colonPos >= 0);
            }
        }

        var port = NO_PORT;
        if (portString != null && portString.length() != 0) {
            // Try to parse the whole port string as a number.
            // JDK7 accepts leading plus signs. We don't want to.
            checkArgument(!portString.startsWith("+"), "Unparseable port number: %s", hostPortString);
            try {
                port = Integer.parseInt(portString);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Unparseable port number: " + hostPortString);
            }
            checkArgument(isValidPort(port), "Port number out of range: %s", hostPortString);
        }

        return new HostAndPort(host, port, hasBracketlessColons);
    }

    /**
     * Parses a bracketed host-port string, throwing IllegalArgumentException if parsing fails.
     *
     * @param hostPortString the full bracketed host-port specification. Post might not be specified.
     * @return an array with 2 strings: host and port, in that order.
     * @throws IllegalArgumentException if parsing the bracketed host-port string fails.
     */
    private static String[] getHostAndPortFromBracketedHost(String hostPortString) {
        int colonIndex;
        int closeBracketIndex;
        checkArgument(
                hostPortString.charAt(0) == '[',
                "Bracketed host-port string must start with a bracket: %s",
                hostPortString);
        colonIndex = hostPortString.indexOf(':');
        closeBracketIndex = hostPortString.lastIndexOf(']');
        checkArgument(
                colonIndex > -1 && closeBracketIndex > colonIndex,
                "Invalid bracketed host/port: %s",
                hostPortString);

        var host = hostPortString.substring(1, closeBracketIndex);
        if (closeBracketIndex + 1 == hostPortString.length()) {
            return new String[]{host, ""};
        } else {
            checkArgument(
                    hostPortString.charAt(closeBracketIndex + 1) == ':',
                    "Only a colon may follow a close bracket: %s",
                    hostPortString);
            for (var i = closeBracketIndex + 2; i < hostPortString.length(); ++i) {
                checkArgument(
                        Character.isDigit(hostPortString.charAt(i)),
                        "Port must be numeric: %s",
                        hostPortString);
            }
            return new String[]{host, hostPortString.substring(closeBracketIndex + 2)};
        }
    }

    /**
     * Return true for valid port numbers.
     */
    private static boolean isValidPort(int port) {
        return port >= 0 && port <= 65535;
    }

    /** Hostname, IPv4/IPv6 literal, or unvalidated nonsense. */
    private final String host;

    /** Validated port number in the range [0..65535], or NO_PORT */
    private final int port;

    /** True if the parsed host has colons, but no surrounding brackets. */
    private final boolean hasBracketlessColons;

    private HostAndPort(String host, int port, boolean hasBracketlessColons) {
        this.host = host;
        this.port = port;
        this.hasBracketlessColons = hasBracketlessColons;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof HostAndPort that) {
            return Objects.equals(this.host, that.host) && this.port == that.port;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port);
    }

    /**
     * Returns the portion of this {@code HostAndPort} instance that should represent the hostname or IPv4/IPv6
     * literal.
     *
     * <p>A successful parse does not imply any degree of sanity in this field.
     */
    public String host() {
        return host;
    }

    /**
     * Get the current port number, failing if no port is defined.
     *
     * @return a validated port number, in the range [0..65535]
     * @throws IllegalStateException if no port is defined.
     */
    public int port() {
        checkState(hasPort());
        return port;
    }

    /**
     * Return true if this instance has a defined port.
     */
    public boolean hasPort() {
        return port >= 0;
    }

    /**
     * Rebuild the host:port string, including brackets if necessary.
     */
    @Override
    public String toString() {
        // "[]:12345" requires 8 extra bytes.
        var builder = new StringBuilder(host.length() + 8);
        if (host.indexOf(':') >= 0) {
            builder.append('[').append(host).append(']');
        } else {
            builder.append(host);
        }
        if (hasPort()) {
            builder.append(':').append(port);
        }
        return builder.toString();
    }
}
