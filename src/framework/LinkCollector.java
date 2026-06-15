package framework;

import java.util.HashSet;
import java.util.Set;

public class LinkCollector {
    private static Set<String> uris = new HashSet<>();

    public static void add(String uri) {
        uris.add(uri);
    }

    public static Set<String> getAll() {
        return uris;
    }
}