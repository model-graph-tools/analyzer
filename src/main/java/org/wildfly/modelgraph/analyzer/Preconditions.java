package org.wildfly.modelgraph.analyzer;

/**
 * Static convenience methods that help a method or constructor check whether it was invoked correctly (that is, whether
 * its <i>preconditions</i> were met).
 *
 * <p>If the precondition is not met, the {@code Preconditions} method throws an unchecked exception
 * of a specified type, which helps the method in which the exception was thrown communicate that its caller has made a
 * mistake. This allows constructs such as
 *
 * <pre>{@code
 * public static double sqrt(double value) {
 *   if (value < 0) {
 *     throw new IllegalArgumentException("input is negative: " + value);
 *   }
 *   // calculate square root
 * }
 * }</pre>
 *
 * <p>to be replaced with the more compact
 *
 * <pre>{@code
 * public static double sqrt(double value) {
 *   checkArgument(value >= 0, "input is negative: %s", value);
 *   // calculate square root
 * }
 * }</pre>
 *
 * <p>so that a hypothetical bad caller of this method, such as:
 *
 * <pre>{@code
 * void exampleBadCaller() {
 *   double d = sqrt(-1.0);
 * }
 * }</pre>
 *
 * <p>would be flagged as having called {@code sqrt()} with an illegal argument.
 *
 * <h3>Performance</h3>
 *
 * <p>Avoid passing message arguments that are expensive to compute; your code will always compute
 * them, even though they usually won't be needed. If you have such arguments, use the conventional if/throw idiom
 * instead.
 *
 * <p>Depending on your message arguments, memory may be allocated for boxing and varargs array
 * creation. However, the methods of this class have a large number of overloads that prevent such allocations in many
 * common cases.
 *
 * <p>The message string is not formatted unless the exception will be thrown, so the cost of the
 * string formatting itself should not be a concern.
 *
 * <p>As with any performance concerns, you should consider profiling your code (in a production
 * environment if possible) before spending a lot of effort on tweaking a particular element.
 *
 * <h3>Other types of preconditions</h3>
 *
 * <p>Not every type of precondition failure is supported by these methods. Continue to throw
 * standard JDK exceptions such as {@link java.util.NoSuchElementException} or {@link UnsupportedOperationException} in
 * the situations they are intended for.
 *
 * <h3>Non-preconditions</h3>
 *
 * <p>It is of course possible to use the methods of this class to check for invalid conditions
 * which are <i>not the caller's fault</i>. Doing so is <b>not recommended</b> because it is misleading to future
 * readers of the code and of stack traces. See <a href="https://github.com/google/guava/wiki/ConditionalFailuresExplained">Conditional
 * failures explained</a> in the Guava User Guide for more advice.
 *
 * <h3>{@code java.util.Objects.requireNonNull()}</h3>
 *
 * <h3>Only {@code %s} is supported</h3>
 *
 * <h3>More information</h3>
 *
 * <p>See the Guava User Guide on <a
 * href="https://github.com/google/guava/wiki/PreconditionsExplained">using {@code Preconditions}</a>.
 *
 * @author Kevin Bourrillion
 * @since 2.0
 */
public final class Preconditions {

    private Preconditions() {
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * @since 20.0 (varargs overload since 2.0)
     */
    public static void checkArgument(boolean b, String errorMessageTemplate, int p1) {
        if (!b) {
            throw new IllegalArgumentException(String.format(errorMessageTemplate, p1));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * @since 20.0 (varargs overload since 2.0)
     */
    public static void checkArgument(boolean b, String errorMessageTemplate, Object p1) {
        if (!b) {
            throw new IllegalArgumentException(String.format(errorMessageTemplate, p1));
        }
    }

    /**
     * Ensures the truth of an expression involving the state of the calling instance, but not involving any parameters
     * to the calling method.
     *
     * @param expression a boolean expression
     * @throws IllegalStateException if {@code expression} is false
     */
    public static void checkState(boolean expression) {
        if (!expression) {
            throw new IllegalStateException();
        }
    }

    /**
     * Ensures that an object reference passed as a parameter to the calling method is not null.
     *
     * @param reference an object reference
     * @return the non-null reference that was validated
     * @throws NullPointerException if {@code reference} is null
     */
    public static <T> T checkNotNull(T reference) {
        if (reference == null) {
            throw new NullPointerException();
        }
        return reference;
    }
}
