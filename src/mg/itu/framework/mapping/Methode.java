package mg.itu.framework.mapping;

public class Methode {
    private String className;
    private String methodName;

    public Methode(String className, String methodName) {
        this.className = className;
        this.methodName = methodName;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }
}
