package org.wildfly.modelgraph.analyzer;

public final class Strings {

    public static boolean isEmpty(String string) {
        return string == null || string.length() == 0;
    }

    private Strings() {}
}
