package mg.itu.Servlet;

public class MethodInfo {
    public String className;
    public String methodName;
    public String url;

    public MethodInfo(String className, String methodName, String url) {
        this.className = className;
        this.methodName = methodName;
        this.url = url;
    }
}