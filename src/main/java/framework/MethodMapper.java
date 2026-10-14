package framework;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
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
import javax.servlet.http.Part;

import java.io.File;
import framework.type.Fichier;

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
            } else if (paramType == Fichier.class) {
                args[i] = extractFichier(req, param.getName());
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
            } else if (fieldType == Fichier.class) {
                Fichier fichier = extractFichier(req, fullPathKey);
                if (fichier == null && prefix != null) {
                    fichier = extractFichier(req, field.getName());
                }
                if (fichier != null) {
                    field.set(instance, fichier);
                }
            } else if (isCollectionOrArray(fieldType)) {
                if (hasMatchingParameterOrPart(req, fullPathKey)) {
                    Object collectionObj = populateCollectionOrArray(fieldType, field.getGenericType(), req, fullPathKey);
                    field.set(instance, collectionObj);
                }
            } else { 
                if (hasMatchingParameterOrPart(req, fullPathKey)) {
                    Object nestedInstance = populateObject(fieldType, req, fullPathKey);
                    field.set(instance, nestedInstance);
                }
            }
        }

        return instance;
    }

    private static Object populateCollectionOrArray(Class<?> targetType, Type genericType, HttpServletRequest req, String prefix) throws Exception {
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

        // Cas 1 : Types simples
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

        // Cas 2 : Fichiers multiples (ex: List<Fichier> ou Fichier[])
        if (elementType == Fichier.class) {
            List<Fichier> tempFichiers = new ArrayList<>();
            int i = 0;
            while (true) {
                String itemPrefix1 = prefix + "[" + i + "]";
                String itemPrefix2 = prefix + "-" + i;
                
                Fichier f = extractFichier(req, itemPrefix1);
                if (f == null) {
                    f = extractFichier(req, itemPrefix2);
                }
                // Si on a un input de type file avec attribut name="prefix[]" géré par les parts multiples
                if (f == null && i == 0) {
                    try {
                        Collection<Part> parts = req.getParts();
                        for (Part p : parts) {
                            if (p.getName().equals(prefix) || p.getName().equals(prefix + "[]")) {
                                if (p.getSize() > 0) {
                                    tempFichiers.add(convertPartToFichier(p));
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                }

                if (f != null) {
                    tempFichiers.add(f);
                } else if (tempFichiers.isEmpty() && f == null && i > 0) {
                    // S'il n'y a pas de format indicé [i], on arrête
                    break;
                }

                if (f == null && i > 0 && tempFichiers.size() == 0) {
                    break;
                }
                
                // Si on a évalué les parts directes, pas besoin de boucler indéfiniment si aucune correspondance indicée
                if (f == null && i > 0) {
                    break;
                }
                i++;
                if (i > 1000) break;
            }

            if (!tempFichiers.isEmpty()) {
                if (targetType.isArray()) {
                    Object array = Array.newInstance(Fichier.class, tempFichiers.size());
                    for (int j = 0; j < tempFichiers.size(); j++) {
                        Array.set(array, j, tempFichiers.get(j));
                    }
                    return array;
                } else {
                    Collection<Object> collection = createCollectionInstance(targetType);
                    collection.addAll(tempFichiers);
                    return collection;
                }
            }
        }

        // Cas 3 : Objets complexes
        List<Object> tempElements = new ArrayList<>();
        int i = 0;
        while (true) {
            String itemPrefix = prefix + "[" + i + "]";

            if (hasMatchingParameterOrPart(req, itemPrefix)) {
                Object item = populateObject(elementType, req, itemPrefix);
                tempElements.add(item);
            } else {
                break;
            }
            i++;
            if (i > 1000) break;
        }

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

    
    private static Fichier extractFichier(HttpServletRequest req, String paramName) {
        try {
            Part part = req.getPart(paramName);
            if (part != null && part.getSize() > 0) {
                return convertPartToFichier(part);
            }
        } catch (Exception e) {
            
        }
        return null;
    }

    private static Fichier convertPartToFichier(Part part) throws Exception {
        String submittedFileName = extractFileName(part);
        String name = "";
        String extension = "";

        if (submittedFileName != null && !submittedFileName.isEmpty()) {
            // Nettoyage du chemin (certains navigateurs envoient le chemin complet du fichier)
            File f = new File(submittedFileName);
            String pureName = f.getName();
            
            int lastDot = pureName.lastIndexOf('.');
            if (lastDot > 0) {
                name = pureName.substring(0, lastDot);
                extension = pureName.substring(lastDot + 1);
            } else {
                name = pureName;
                extension = "";
            }
        }

        byte[] binaryBytes;
        try (InputStream is = part.getInputStream();
             ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            int nRead;
            byte[] data = new byte[16384];
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            binaryBytes = buffer.toByteArray();
        }

        return new Fichier(name, extension, binaryBytes);
    }

    private static String extractFileName(Part part) {
        String contentDisp = part.getHeader("content-disposition");
        for (String cd : contentDisp.split(";")) {
            if (cd.trim().startsWith("filename")) {
                String fileName = cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
                return fileName;
            }
        }
        return null;
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

    private static boolean hasMatchingParameterOrPart(HttpServletRequest req, String prefix) {
        String searchPrefix1 = prefix + "-";
        String searchPrefix2 = prefix + "[";
        
        // Vérification dans les paramètres classiques
        java.util.Enumeration<String> paramNames = req.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String name = paramNames.nextElement();
            if (name.startsWith(searchPrefix1) || name.startsWith(searchPrefix2) || name.equals(prefix)) {
                return true;
            }
        }

        // Vérification dans les parts (fichiers)
        try {
            for (Part part : req.getParts()) {
                String name = part.getName();
                if (name.startsWith(searchPrefix1) || name.startsWith(searchPrefix2) || name.equals(prefix)) {
                    return true;
                }
            }
        } catch (Exception ignored) {}

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