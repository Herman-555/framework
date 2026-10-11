package framework;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

public class MethodMapper {

    public static Object[] resolveParameters(Method method, HttpServletRequest req) throws Exception {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            Class<?> paramType = param.getType();

            if (isSimpleType(paramType)) {
                String paramName = param.getName();
                String rawValue = req.getParameter(paramName);
                args[i] = convertValue(rawValue, paramType);
            } else if (isCollectionOrArray(paramType)) {
                args[i] = populateCollectionOrArray(paramType, param.getParameterizedType(), req, param.getName());
            } else {
                args[i] = populateObject(paramType, req, param.getName());
            }
        }

        return args;
    }

    private static Object populateObject(Class<?> clazz, HttpServletRequest req, String prefix) throws Exception {
        Object instance = clazz.getDeclaredConstructor().newInstance();

        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            
            // Construit la clé au format prefix-champ (ex: listepersonne[0]-nom)
            String fullPathKey = (prefix != null && !prefix.isEmpty()) 
                    ? prefix + "-" + field.getName() 
                    : field.getName();

            Class<?> fieldType = field.getType();

            if (isSimpleType(fieldType)) {
                String rawValue = req.getParameter(fullPathKey);

                if (rawValue == null && prefix != null) {
                    rawValue = req.getParameter(field.getName());
                }

                if (rawValue != null) {
                    Object convertedValue = convertValue(rawValue, fieldType);
                    field.set(instance, convertedValue);
                }
            } else if (isCollectionOrArray(fieldType)) {
                if (hasMatchingParameter(req, fullPathKey)) {
                    Object collectionObj = populateCollectionOrArray(fieldType, field.getGenericType(), req, fullPathKey);
                    field.set(instance, collectionObj);
                }
            } else { 
                if (hasMatchingParameter(req, fullPathKey)) {
                    Object nestedInstance = populateObject(fieldType, req, fullPathKey);
                    field.set(instance, nestedInstance);
                }
            }
        }

        return instance;
    }

    private static Object populateCollectionOrArray(Class<?> targetType, Type genericType, HttpServletRequest req, String prefix) throws Exception {
        // 1. Déterminer le type des éléments de la collection/tableau
        Class<?> elementType = Object.class;
        if (genericType instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) genericType;
            Type actualArg = pt.getActualTypeArguments()[0];
            if (actualArg instanceof Class) {
                elementType = (Class<?>) actualArg;
            }
        } else if (targetType.isArray()) {
            elementType = targetType.getComponentType();
        }

        // 2. Cas des types simples (ex: List<String> ou String[])
        if (isSimpleType(elementType)) {
            String[] values = req.getParameterValues(prefix);
            if (values == null) {
                values = req.getParameterValues(prefix + "[]");
            }

            if (values != null) {
                if (targetType.isArray()) {
                    Object array = Array.newInstance(elementType, values.length);
                    for (int i = 0; i < values.length; i++) {
                        Array.set(array, i, convertValue(values[i], elementType));
                    }
                    return array;
                } else {
                    Collection<Object> collection = createCollectionInstance(targetType);
                    for (String val : values) {
                        collection.add(convertValue(val, elementType));
                    }
                    return collection;
                }
            }
        }

        // 3. Cas des objets complexes (ex: List<Personne>) avec format listepersonne[0]-nom
        List<Object> tempElements = new ArrayList<>();
        int i = 0;
        while (true) {
            // Le préfixe de l'élément individuel devient: listepersonne[0]
            String itemPrefix = prefix + "[" + i + "]";

            if (hasMatchingParameter(req, itemPrefix)) {
                // populateObject va combiner itemPrefix avec le nom du champ en utilisant un tiret (-)
                // Ce qui donnera: listepersonne[0]-nom, listepersonne[0]-prenom, etc.
                Object item = populateObject(elementType, req, itemPrefix);
                tempElements.add(item);
            } else {
                break;
            }
            i++;
            if (i > 1000) break; // Sécurité anti-boucle infinie
        }

        // 4. Restitution en Tableau ou Collection
        if (targetType.isArray()) {
            Object array = Array.newInstance(elementType, tempElements.size());
            for (int j = 0; j < tempElements.size(); j++) {
                Array.set(array, j, tempElements.get(j));
            }
            return array;
        } else {
            Collection<Object> collection = createCollectionInstance(targetType);
            collection.addAll(tempElements);
            return collection;
        }
    }

    private static Collection<Object> createCollectionInstance(Class<?> collectionType) {
        if (Set.class.isAssignableFrom(collectionType)) {
            return new HashSet<>();
        }
        return new ArrayList<>();
    }

    private static boolean isCollectionOrArray(Class<?> type) {
        return type.isArray() || Collection.class.isAssignableFrom(type);
    }

    private static boolean hasMatchingParameter(HttpServletRequest req, String prefix) {
        String searchPrefix1 = prefix + "-";
        String searchPrefix2 = prefix + "[";
        java.util.Enumeration<String> paramNames = req.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String name = paramNames.nextElement();
            if (name.startsWith(searchPrefix1) || name.startsWith(searchPrefix2) || name.equals(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSimpleType(Class<?> type) {
        return type.isPrimitive() 
            || type == String.class 
            || type == Integer.class 
            || type == Double.class 
            || type == Float.class 
            || type == Long.class 
            || type == Boolean.class;
    }

    private static Object convertValue(String value, Class<?> targetType) {
        if (value == null || value.trim().isEmpty()) {
            return getDefaultValue(targetType);
        }

        try {
            if (targetType == String.class) {
                return value;
            } else if (targetType == int.class || targetType == Integer.class) {
                return Integer.parseInt(value);
            } else if (targetType == double.class || targetType == Double.class) {
                return Double.parseDouble(value);
            } else if (targetType == float.class || targetType == Float.class) {
                return Float.parseFloat(value);
            } else if (targetType == long.class || targetType == Long.class) {
                return Long.parseLong(value);
            } else if (targetType == boolean.class || targetType == Boolean.class) {
                return Boolean.parseBoolean(value);
            }
        } catch (NumberFormatException e) {
            return getDefaultValue(targetType);
        }

        return null;
    }

    private static Object getDefaultValue(Class<?> type) {
        if (type == int.class) return 0;
        if (type == double.class) return 0.0;
        if (type == float.class) return 0.0f;
        if (type == long.class) return 0L;
        if (type == boolean.class) return false;
        return null;
    }
}