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
                
                args[i] = populateObject(paramType, req);

            }
        }

        return args;
    }

    /**
     * Instancier objet et remplir attributs depuis arguments de req http.
     */
    private static Object populateObject(Class<?> clazz, HttpServletRequest req) throws Exception {
        
        Object instance = clazz.getDeclaredConstructor().newInstance(); //Creer instance

        // Parcourir champ de la classe.
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true); // champs private.
            
            String fieldName = field.getName();
            String rawValue = req.getParameter(fieldName);

            if (rawValue != null) {
                Object convertedValue = convertValue(rawValue, field.getType());
                field.set(instance, convertedValue);
            }
        }

        return instance;
    }

    private static boolean isSimpleType(Class<?> type) { // Est primitif
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