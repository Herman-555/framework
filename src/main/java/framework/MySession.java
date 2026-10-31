package framework;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MySession {
    public static final String AUTHENTICATED = "authenticated";
    public static final String ROLES = "roles";
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

    public void login(String... roles) {
        set(AUTHENTICATED, Boolean.TRUE);
        Set<String> sessionRoles = new HashSet<>();
        if (roles != null) {
            for (String role : roles) {
                if (role != null && !role.trim().isEmpty()) {
                    sessionRoles.add(role.trim());
                }
            }
        }
        set(ROLES, sessionRoles);
    }

    public boolean isAuthenticated() {
        return Boolean.TRUE.equals(get(AUTHENTICATED));
    }

    public boolean hasRole(String role) {
        Object value = get(ROLES);
        return value instanceof Set && role != null && ((Set<?>) value).contains(role);
    }
}