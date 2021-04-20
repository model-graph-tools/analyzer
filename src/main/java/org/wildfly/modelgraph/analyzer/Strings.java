package org.wildfly.modelgraph.analyzer;

public final class Strings {

    public static boolean isEmpty(String string) {
        return string == null || string.length() == 0;
    }

    public static String identify(String string) {
        if (string != null) {
            return string.toLowerCase().replace(' ', '-');
        }
        return "";
    }

    private Strings() {}
}
