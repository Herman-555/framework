package framework;

import java.util.HashMap;
import java.util.Map;

public class MySession {
    private Map<String, Object> attributes = new HashMap<>();

    public Object get(String key) {
        return attributes.get(key);
    }

    public void set(String key, Object value) {
        attributes.put(key, value);
    }

    public void remove(String key) {
        attributes.remove(key);
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void clear() {
        attributes.clear();
    }
}