package framework;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
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
            } else {
                
                args[i] = populateObject(paramType, req, param.getName());
            }
        }

        return args;
    }

    // Convention : prefix-champ1-champ2. Traiter objet non primitif.
    private static Object populateObject(Class<?> clazz, HttpServletRequest req, String prefix) throws Exception {
        Object instance = clazz.getDeclaredConstructor().newInstance();

        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            
            // Creer la cle complete pour la recherche de param.
            String fullPathKey = (prefix != null && !prefix.isEmpty()) 
                    ? prefix + "-" + field.getName() 
                    : field.getName();

            Class<?> fieldType = field.getType();

            if (isSimpleType(fieldType)) {
                
                String rawValue = req.getParameter(fullPathKey);

                // Si non trouvé et qu'un préfixe était présent, tente une recherche par le nom de champ simple ("lieu")
                if (rawValue == null && prefix != null) {
                    rawValue = req.getParameter(field.getName());
                }

                if (rawValue != null) {
                    Object convertedValue = convertValue(rawValue, fieldType);
                    field.set(instance, convertedValue);
                }

            } else { //Objet imbrique
                
                // verifier si au moins une param de requete contient au depart ce prefix
                if (hasMatchingParameter(req, fullPathKey)) {
                    Object nestedInstance = populateObject(fieldType, req, fullPathKey);
                    field.set(instance, nestedInstance);
                }
            }
        }

        return instance;
    }

    //Verifier si la requete contient au moins un parametre commencant par ce prefixe (ex: "user-profil-")
    private static boolean hasMatchingParameter(HttpServletRequest req, String prefix) {
        String searchPrefix = prefix + "-";
        java.util.Enumeration<String> paramNames = req.getParameterNames();
        while (paramNames.hasMoreElements()) {
            if (paramNames.nextElement().startsWith(searchPrefix)) {
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